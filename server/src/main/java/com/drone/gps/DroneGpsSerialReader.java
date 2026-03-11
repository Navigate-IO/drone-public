package com.drone.gps;

import com.drone.Config;
import com.drone.MessengerUtils;
import com.drone.Utils;
import com.drone.data.DroneGpsReading;
import com.drone.data.DroneGpsRelayMessage;
import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DroneGpsSerialReader {
    private static final String DEFAULT_SERIAL_DEVICE = "/dev/ttyUSB0";
    private static final int DEFAULT_BAUD_RATE = 9600;
    private static final long DEFAULT_PUBLISH_INTERVAL_MS = 60_000L;
    private static final long RECONNECT_DELAY_MS = 3000L;
    private static final String[] COMMON_LINUX_PORTS = new String[] {
        "/dev/ttyUSB0",
        "/dev/ttyACM0",
        "/dev/serial0",
        "/dev/ttyAMA0",
        "/dev/ttyS0"
    };

    private static final DroneGpsSerialReader INSTANCE = new DroneGpsSerialReader();

    private final AtomicReference<DroneGpsReading> latestReading = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean placeholderPublished = new AtomicBoolean(false);
    private final Object logLock = new Object();

    private final Path logFilePath = Path.of("drone_gps_log.txt");

    private Thread workerThread;
    private volatile SerialPort activePort;
    private volatile String activePortPath;
    private volatile boolean autoPortScan;
    private volatile long publishIntervalMs = DEFAULT_PUBLISH_INTERVAL_MS;
    private volatile long lastPublishedAtMs = 0L;

    private volatile LocalDate lastUtcDateFromRmc;
    private Double lastLatitude;
    private Double lastLongitude;
    private Double lastAltitude;
    private Double lastSpeed;
    private Integer lastFixQuality;
    private Integer lastSatelliteCount;
    private String lastUtcTimestamp;

    private DroneGpsSerialReader() {
    }

    public static DroneGpsSerialReader getInstance() {
        return INSTANCE;
    }

    public synchronized void start(String serialDevice, int baudRate) {
        start(serialDevice, baudRate, false, DEFAULT_PUBLISH_INTERVAL_MS);
    }

    public synchronized void start(String serialDevice, int baudRate, boolean autoPortScan) {
        start(serialDevice, baudRate, autoPortScan, DEFAULT_PUBLISH_INTERVAL_MS);
    }

    public synchronized void start(String serialDevice, int baudRate, boolean autoPortScan, long publishIntervalMs) {
        if (running.get()) {
            return;
        }

        final String deviceToUse = (serialDevice == null || serialDevice.isBlank())
            ? DEFAULT_SERIAL_DEVICE : serialDevice;
        final int baudToUse = baudRate > 0 ? baudRate : DEFAULT_BAUD_RATE;
        this.autoPortScan = autoPortScan;
        this.publishIntervalMs = publishIntervalMs > 0 ? publishIntervalMs : DEFAULT_PUBLISH_INTERVAL_MS;
        this.lastPublishedAtMs = 0L;

        running.set(true);
        workerThread = new Thread(() -> runLoop(deviceToUse, baudToUse), "drone-gps-serial-reader");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public synchronized void stop() {
        running.set(false);

        SerialPort serialPort = activePort;
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }

        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public DroneGpsReading getLatestReading() {
        return latestReading.get();
    }

    private void runLoop(String serialDevice, int baudRate) {
        while (running.get()) {
            SerialPort serialPort = openAvailablePort(serialDevice, baudRate, autoPortScan);
            if (serialPort == null) {
                System.err.println(
                    "[DRONE GPS] No GPS serial port opened. Preferred="
                        + serialDevice + " autoScan=" + autoPortScan
                        + " available=" + listAvailablePortPaths() + " (retrying)"
                );
                publishPlaceholderIfNeeded("NO_SENSOR");
                sleepQuietly(RECONNECT_DELAY_MS);
                continue;
            }

            activePort = serialPort;
            placeholderPublished.set(false);
            System.out.println("[DRONE GPS] Connected to " + activePortPath + " @ " + baudRate + " baud");

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(serialPort.getInputStream(), StandardCharsets.US_ASCII))) {

                while (running.get() && serialPort.isOpen()) {
                    String line = reader.readLine();
                    if (line == null) {
                        continue;
                    }

                    handleIncomingText(line);
                }
            } catch (IOException exception) {
                if (running.get()) {
                    System.err.println("[DRONE GPS] Serial read error: " + exception.getMessage());
                    publishPlaceholderIfNeeded("READ_ERROR");
                }
            } finally {
                if (serialPort.isOpen()) {
                    serialPort.closePort();
                }
                activePort = null;
                activePortPath = null;
            }

            if (running.get()) {
                System.err.println("[DRONE GPS] Serial connection lost, reconnecting...");
                sleepQuietly(RECONNECT_DELAY_MS);
            }
        }
    }

    private DroneGpsReading processSentence(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return null;
        }

        sentence = sanitizeSentence(sentence);
        int start = sentence.indexOf('$');
        if (start < 0) {
            return null;
        }
        if (start > 0) {
            sentence = sentence.substring(start);
        }
        if (sentence.isBlank() || !sentence.startsWith("$")) {
            return null;
        }

        if (!isChecksumValid(sentence)) {
            return null;
        }

        int checksumIndex = sentence.indexOf('*');
        String payload = checksumIndex > 0 ? sentence.substring(0, checksumIndex) : sentence;
        String[] fields = payload.split(",", -1);
        if (fields.length == 0) {
            return null;
        }

        for (int i = 0; i < fields.length; i++) {
            fields[i] = sanitizeField(fields[i]);
        }

        String sentenceType = fields[0].toUpperCase();
        try {
            if (isRmcSentence(sentenceType)) {
                return parseRmc(fields);
            }
            if (isGgaSentence(sentenceType)) {
                return parseGga(fields);
            }
        } catch (RuntimeException exception) {
            System.err.println("[DRONE GPS] Ignoring malformed sentence: " + sentence);
        }

        return null;
    }

    private void handleIncomingText(String incomingText) {
        for (String sentence : splitIntoSentenceCandidates(incomingText)) {
            DroneGpsReading reading = processSentence(sentence);
            if (reading == null) {
                continue;
            }

            latestReading.set(reading);
            if (shouldPublish(reading.getLocalTimestamp())) {
                publishReading(reading);
            }
        }
    }

    private boolean shouldPublish(long currentTimestampMs) {
        if (lastPublishedAtMs <= 0) {
            lastPublishedAtMs = currentTimestampMs;
            return true;
        }
        if (currentTimestampMs - lastPublishedAtMs >= publishIntervalMs) {
            lastPublishedAtMs = currentTimestampMs;
            return true;
        }
        return false;
    }

    private void publishReading(DroneGpsReading reading) {
        printReading(reading);
        appendToLog(reading);
        sendReadingToOtherDrones(reading);
    }

    private void sendReadingToOtherDrones(DroneGpsReading reading) {
        if (Config.getInstance() == null) {
            return;
        }
        DroneGpsRelayMessage relayMessage = new DroneGpsRelayMessage("drone-gps", reading);
        String relayJson = Utils.toJson(relayMessage);
        MessengerUtils.sendToDrones(relayJson);
    }

    private List<String> splitIntoSentenceCandidates(String incomingText) {
        ArrayList<String> sentences = new ArrayList<>();
        if (incomingText == null) {
            return sentences;
        }

        String normalized = incomingText.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.isEmpty()) {
            return sentences;
        }

        if (!normalized.contains("$")) {
            sentences.add(normalized);
            return sentences;
        }

        String[] parts = normalized.split("\\$");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            sentences.add("$" + trimmed);
        }

        return sentences;
    }

    private DroneGpsReading parseRmc(String[] fields) {
        if (fields.length < 10) {
            return null;
        }

        String status = fields[2];
        Double latitude = parseCoordinate(fields[3], fields[4]);
        Double longitude = parseCoordinate(fields[5], fields[6]);
        Double speed = parseDouble(fields[7]);

        LocalDate utcDate = parseDate(fields[9]);
        if (utcDate != null) {
            lastUtcDateFromRmc = utcDate;
        }

        String utcTimestamp = buildUtcTimestamp(utcDate, fields[1]);
        if (utcTimestamp != null) {
            lastUtcTimestamp = utcTimestamp;
        }

        if ("A".equalsIgnoreCase(status)) {
            if (latitude != null) {
                lastLatitude = latitude;
            }
            if (longitude != null) {
                lastLongitude = longitude;
            }
            if (speed != null) {
                lastSpeed = speed;
            }
            if (lastFixQuality == null || lastFixQuality <= 0) {
                lastFixQuality = 1;
            }
        } else {
            lastFixQuality = 0;
            if (isBlank(fields[3])) {
                lastLatitude = null;
            } else if (latitude != null) {
                lastLatitude = latitude;
            }
            if (isBlank(fields[5])) {
                lastLongitude = null;
            } else if (longitude != null) {
                lastLongitude = longitude;
            }
            if (isBlank(fields[7])) {
                lastSpeed = null;
            } else if (speed != null) {
                lastSpeed = speed;
            }
        }

        return buildReading();
    }

    private DroneGpsReading parseGga(String[] fields) {
        if (fields.length < 10) {
            return null;
        }

        Double latitude = parseCoordinate(fields[2], fields[3]);
        Double longitude = parseCoordinate(fields[4], fields[5]);
        Integer fixQuality = parseInteger(fields[6]);
        Integer satelliteCount = parseInteger(fields[7]);
        Double altitude = parseDouble(fields[9]);

        String utcTimestamp = buildUtcTimestamp(lastUtcDateFromRmc, fields[1]);
        if (utcTimestamp != null) {
            lastUtcTimestamp = utcTimestamp;
        }

        if (fixQuality != null && fixQuality <= 0) {
            lastFixQuality = fixQuality;

            if (isBlank(fields[2])) {
                lastLatitude = null;
            } else if (latitude != null) {
                lastLatitude = latitude;
            }
            if (isBlank(fields[4])) {
                lastLongitude = null;
            } else if (longitude != null) {
                lastLongitude = longitude;
            }
            if (isBlank(fields[9])) {
                lastAltitude = null;
            } else if (altitude != null) {
                lastAltitude = altitude;
            }
        } else {
            if (latitude != null) {
                lastLatitude = latitude;
            }
            if (longitude != null) {
                lastLongitude = longitude;
            }
            if (altitude != null) {
                lastAltitude = altitude;
            }
            if (fixQuality != null) {
                lastFixQuality = fixQuality;
            }
        }

        if (satelliteCount != null) {
            lastSatelliteCount = satelliteCount;
        }

        return buildReading();
    }

    private DroneGpsReading buildReading() {
        long localTimestamp = System.currentTimeMillis();
        return new DroneGpsReading(
            lastLatitude,
            lastLongitude,
            lastAltitude,
            lastSpeed,
            lastFixQuality,
            lastSatelliteCount,
            lastUtcTimestamp,
            localTimestamp
        );
    }

    private void printReading(DroneGpsReading reading) {
        String line = String.format(
            "[DRONE GPS] lat=%s lon=%s alt(m)=%s speed(knots)=%s fix=%s sats=%s utc=%s local=%d",
            formatDouble(reading.getLatitude(), 6),
            formatDouble(reading.getLongitude(), 6),
            formatDouble(reading.getAltitude(), 2),
            formatDouble(reading.getSpeed(), 2),
            formatInteger(reading.getFixQuality()),
            formatInteger(reading.getSatelliteCount()),
            formatString(reading.getUtcTimestamp()),
            reading.getLocalTimestamp()
        );
        System.out.println(line);
    }

    private void appendToLog(DroneGpsReading reading) {
        synchronized (logLock) {
            boolean fileExists = Files.exists(logFilePath);
            try (BufferedWriter writer = Files.newBufferedWriter(
                logFilePath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

                if (!fileExists) {
                    writer.write("localTimestamp,utcTimestamp,latitude,longitude,altitude,speed,fixQuality,satelliteCount");
                    writer.newLine();
                }

                String csvLine = String.format(
                    "%d,%s,%s,%s,%s,%s,%s,%s",
                    reading.getLocalTimestamp(),
                    csvValue(reading.getUtcTimestamp()),
                    csvValue(reading.getLatitude()),
                    csvValue(reading.getLongitude()),
                    csvValue(reading.getAltitude()),
                    csvValue(reading.getSpeed()),
                    csvValue(reading.getFixQuality()),
                    csvValue(reading.getSatelliteCount())
                );
                writer.write(csvLine);
                writer.newLine();
            } catch (IOException exception) {
                System.err.println("[DRONE GPS] Failed to append log: " + exception.getMessage());
            }
        }
    }

    private String csvValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sanitizeSentence(String sentence) {
        return sentence.replaceAll("[^\\x20-\\x7E]", "").trim();
    }

    private String sanitizeField(String field) {
        if (field == null) {
            return "";
        }
        return field.replaceAll("[^\\x20-\\x7E]", "").trim();
    }

    private SerialPort openAvailablePort(String preferredPortPath, int baudRate, boolean autoScan) {
        if (!autoScan) {
            SerialPort candidate = SerialPort.getCommPort(preferredPortPath);
            configure(candidate, baudRate);
            if (candidate.openPort()) {
                activePortPath = preferredPortPath;
                return candidate;
            }
            return null;
        }

        for (String path : buildCandidatePortPaths(preferredPortPath)) {
            SerialPort candidate = SerialPort.getCommPort(path);
            configure(candidate, baudRate);
            if (candidate.openPort()) {
                activePortPath = path;
                return candidate;
            }
        }
        return null;
    }

    private Set<String> buildCandidatePortPaths(String preferredPortPath) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (preferredPortPath != null && !preferredPortPath.isBlank()) {
            candidates.add(preferredPortPath.trim());
        }

        for (String path : COMMON_LINUX_PORTS) {
            candidates.add(path);
        }

        for (SerialPort port : SerialPort.getCommPorts()) {
            String path = toPortPath(port);
            if (path != null && !path.isBlank()) {
                candidates.add(path);
            }
        }
        return candidates;
    }

    private String listAvailablePortPaths() {
        StringBuilder result = new StringBuilder();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (int index = 0; index < ports.length; index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(toPortPath(ports[index]));
        }
        return result.toString();
    }

    private String toPortPath(SerialPort port) {
        String path = port.getSystemPortPath();
        if (path == null || path.isBlank()) {
            String name = port.getSystemPortName();
            if (name == null || name.isBlank()) {
                return null;
            }
            return name.startsWith("/dev/") ? name : "/dev/" + name;
        }
        return path;
    }

    private void configure(SerialPort port, int baudRate) {
        port.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
    }

    private boolean isRmcSentence(String sentenceType) {
        return sentenceType != null && sentenceType.startsWith("$") && sentenceType.endsWith("RMC");
    }

    private boolean isGgaSentence(String sentenceType) {
        return sentenceType != null && sentenceType.startsWith("$") && sentenceType.endsWith("GGA");
    }

    private boolean isChecksumValid(String sentence) {
        int asteriskIndex = sentence.indexOf('*');
        if (asteriskIndex <= 0) {
            return true;
        }
        if (asteriskIndex + 2 >= sentence.length()) {
            return false;
        }

        String checksumHex = sentence.substring(asteriskIndex + 1).trim();
        if (checksumHex.length() > 2) {
            checksumHex = checksumHex.substring(0, 2);
        }

        int expected;
        try {
            expected = Integer.parseInt(checksumHex, 16);
        } catch (NumberFormatException exception) {
            return false;
        }

        int calculated = 0;
        for (int i = 1; i < asteriskIndex; i++) {
            calculated ^= sentence.charAt(i);
        }
        return calculated == expected;
    }

    private void publishPlaceholderIfNeeded(String utcStatus) {
        if (!running.get()) {
            return;
        }
        if (!placeholderPublished.compareAndSet(false, true)) {
            return;
        }

        DroneGpsReading placeholder = new DroneGpsReading(
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            -1,
            -1,
            utcStatus,
            System.currentTimeMillis()
        );
        latestReading.set(placeholder);

        System.out.println(
            "[DRONE GPS] lat=-1 lon=-1 alt(m)=-1 speed(knots)=-1 fix=-1 sats=-1 utc="
                + utcStatus + " local=" + placeholder.getLocalTimestamp()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private LocalDate parseDate(String rawDate) {
        String cleaned = extractNumeric(rawDate);
        if (cleaned == null || cleaned.length() < 6) {
            return null;
        }

        try {
            int day = Integer.parseInt(cleaned.substring(0, 2));
            int month = Integer.parseInt(cleaned.substring(2, 4));
            int year = Integer.parseInt(cleaned.substring(4, 6));
            int resolvedYear = year >= 80 ? 1900 + year : 2000 + year;
            return LocalDate.of(resolvedYear, month, day);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String buildUtcTimestamp(LocalDate utcDate, String rawTime) {
        LocalTime time = parseTime(rawTime);
        if (time == null) {
            return lastUtcTimestamp;
        }

        LocalDate dateToUse = utcDate;
        if (dateToUse == null) {
            dateToUse = lastUtcDateFromRmc;
        }
        if (dateToUse == null) {
            dateToUse = LocalDate.now(ZoneOffset.UTC);
        }

        LocalDateTime dateTime = LocalDateTime.of(dateToUse, time);
        return dateTime.toInstant(ZoneOffset.UTC).toString();
    }

    private LocalTime parseTime(String rawTime) {
        if (rawTime == null || rawTime.length() < 6) {
            return null;
        }

        String trimmed = rawTime.replaceAll("[^0-9.]", "").trim();
        if (trimmed.length() < 6) {
            return null;
        }

        try {
            String hhmmss = trimmed.substring(0, 6);
            int hour = Integer.parseInt(hhmmss.substring(0, 2));
            int minute = Integer.parseInt(hhmmss.substring(2, 4));
            int second = Integer.parseInt(hhmmss.substring(4, 6));

            int nanos = 0;
            int dotIndex = trimmed.indexOf('.');
            if (dotIndex >= 0 && dotIndex + 1 < trimmed.length()) {
                String fraction = trimmed.substring(dotIndex + 1);
                if (fraction.length() > 9) {
                    fraction = fraction.substring(0, 9);
                }
                while (fraction.length() < 9) {
                    fraction = fraction + "0";
                }
                nanos = Integer.parseInt(fraction);
            }

            return LocalTime.of(hour, minute, second, nanos);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Double parseCoordinate(String rawCoordinate, String hemisphere) {
        String coordinateToken = extractNumericWithDecimal(rawCoordinate);
        if (coordinateToken == null || coordinateToken.isBlank()) {
            return null;
        }

        String hemisphereToken = extractHemisphere(hemisphere);
        if (hemisphereToken == null) {
            return null;
        }

        try {
            double coordinate = Double.parseDouble(coordinateToken);
            int degrees = (int) (coordinate / 100);
            double minutes = coordinate - (degrees * 100.0);
            double decimalDegrees = degrees + (minutes / 60.0);

            if ("S".equals(hemisphereToken) || "W".equals(hemisphereToken)) {
                decimalDegrees = -decimalDegrees;
            }

            return decimalDegrees;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        String cleaned = extractNumericWithDecimal(value);
        if (cleaned == null || cleaned.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        String cleaned = extractInteger(value);
        if (cleaned == null || cleaned.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String extractNumeric(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[^0-9]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String extractNumericWithDecimal(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[^0-9.\\-+]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String extractInteger(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[^0-9\\-+]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String extractHemisphere(String value) {
        if (value == null) {
            return null;
        }

        String upper = value.toUpperCase();
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c == 'N' || c == 'S' || c == 'E' || c == 'W') {
                return String.valueOf(c);
            }
        }
        return null;
    }

    private String formatDouble(Double value, int precision) {
        if (value == null) {
            return "n/a";
        }
        return String.format("%." + precision + "f", value);
    }

    private String formatInteger(Integer value) {
        return value == null ? "n/a" : String.valueOf(value);
    }

    private String formatString(String value) {
        return value == null || value.isBlank() ? "n/a" : value;
    }
}
