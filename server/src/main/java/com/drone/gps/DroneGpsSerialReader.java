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
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DroneGpsSerialReader {
    private static final String DEFAULT_SERIAL_DEVICE = "/dev/ttyUSB0";
    private static final int DEFAULT_BAUD_RATE = 9600;
    private static final long DEFAULT_PUBLISH_INTERVAL_MS = 60_000L;
    private static final long RECONNECT_DELAY_MS = 15_000L;
    private static final long AUTO_SCAN_PROBE_WINDOW_MS = 3000L;
    private static final int AUTO_SCAN_REQUIRED_SUPPORTED_SENTENCES = 1;
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
    private Double lastTrackAngle;
    private Double lastMagneticVariation;
    private Integer lastFixQuality;
    private Integer lastFixType;
    private Integer lastSatelliteCount;
    private Integer lastSatellitesInViewCount;
    private Double lastHdop;
    private Double lastVdop;
    private Double lastPdop;
    private Double lastGeoidHeight;
    private String lastUtcDateRaw;
    private String lastUtcTimeRaw;
    private final LinkedHashSet<Integer> lastSatellitesInViewPrns = new LinkedHashSet<>();
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
            if (isGsaSentence(sentenceType)) {
                return parseGsa(fields);
            }
            if (isGsvSentence(sentenceType)) {
                return parseGsv(fields);
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
            printReading(reading);
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
        Double trackAngle = parseDouble(fields[8]);
        Double magneticVariation = fields.length > 10 ? parseDouble(fields[10]) : null;
        if (magneticVariation != null && fields.length > 11 && "W".equalsIgnoreCase(fields[11])) {
            magneticVariation = -Math.abs(magneticVariation);
        }

        LocalDate utcDate = parseDate(fields[9]);
        if (utcDate != null) {
            lastUtcDateFromRmc = utcDate;
        }
        lastUtcDateRaw = normalizeRmcDate(fields[9]);
        lastUtcTimeRaw = normalizeUtcTime(fields[1]);

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
            if (trackAngle != null) {
                lastTrackAngle = trackAngle;
            }
            if (magneticVariation != null) {
                lastMagneticVariation = magneticVariation;
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
            if (isBlank(fields[8])) {
                lastTrackAngle = null;
            } else if (trackAngle != null) {
                lastTrackAngle = trackAngle;
            }
            if (fields.length <= 10 || isBlank(fields[10])) {
                lastMagneticVariation = null;
            } else if (magneticVariation != null) {
                lastMagneticVariation = magneticVariation;
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
        Double hdop = parseDouble(fields[8]);
        Double altitude = parseDouble(fields[9]);
        Double geoidHeight = fields.length > 11 ? parseDouble(fields[11]) : null;

        String utcTimestamp = buildUtcTimestamp(lastUtcDateFromRmc, fields[1]);
        if (utcTimestamp != null) {
            lastUtcTimestamp = utcTimestamp;
        }
        lastUtcTimeRaw = normalizeUtcTime(fields[1]);

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
            if (isBlank(fields[8])) {
                lastHdop = null;
            } else if (hdop != null) {
                lastHdop = hdop;
            }
            if (fields.length <= 11 || isBlank(fields[11])) {
                lastGeoidHeight = null;
            } else if (geoidHeight != null) {
                lastGeoidHeight = geoidHeight;
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
            if (hdop != null) {
                lastHdop = hdop;
            }
            if (geoidHeight != null) {
                lastGeoidHeight = geoidHeight;
            }
        }

        if (satelliteCount != null) {
            lastSatelliteCount = satelliteCount;
        }

        return buildReading();
    }

    private DroneGpsReading parseGsa(String[] fields) {
        if (fields.length < 4) {
            return null;
        }

        Integer fixType = parseInteger(fields[2]);
        if (fixType != null) {
            lastFixType = fixType;
        }

        int dopStart = Math.max(3, fields.length - 3);
        Double pdop = fields.length >= dopStart + 1 ? parseDouble(fields[dopStart]) : null;
        Double hdop = fields.length >= dopStart + 2 ? parseDouble(fields[dopStart + 1]) : null;
        Double vdop = fields.length >= dopStart + 3 ? parseDouble(fields[dopStart + 2]) : null;

        if (pdop != null) {
            lastPdop = pdop;
        }
        if (hdop != null) {
            lastHdop = hdop;
        }
        if (vdop != null) {
            lastVdop = vdop;
        }

        return buildReading();
    }

    private DroneGpsReading parseGsv(String[] fields) {
        if (fields.length < 4) {
            return null;
        }

        Integer messageNumber = parseInteger(fields[2]);
        Integer satellitesInView = parseInteger(fields[3]);
        if (satellitesInView != null) {
            lastSatellitesInViewCount = satellitesInView;
        }

        if (messageNumber != null && messageNumber == 1) {
            lastSatellitesInViewPrns.clear();
        }

        for (int index = 4; index < fields.length; index += 4) {
            Integer prn = parseInteger(fields[index]);
            if (prn != null && prn > 0) {
                lastSatellitesInViewPrns.add(prn);
            }
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
            lastTrackAngle,
            lastMagneticVariation,
            lastFixQuality,
            lastFixType,
            lastSatelliteCount,
            lastSatellitesInViewCount,
            new ArrayList<>(lastSatellitesInViewPrns),
            lastHdop,
            lastVdop,
            lastPdop,
            lastGeoidHeight,
            lastUtcDateRaw,
            lastUtcTimeRaw,
            lastUtcTimestamp,
            localTimestamp
        );
    }

    private void printReading(DroneGpsReading reading) {
        String line = String.format(
            "[DRONE GPS] lat=%s lon=%s alt(m)=%s speed(knots)=%s track=%s magVar=%s fixQ=%s fixType=%s satsUsed=%s satsView=%s satPrns=%s hdop=%s vdop=%s pdop=%s geoid=%s utcDate=%s utcTime=%s utc=%s local=%d",
            formatDouble(reading.getLatitude(), 6),
            formatDouble(reading.getLongitude(), 6),
            formatDouble(reading.getAltitude(), 2),
            formatDouble(reading.getSpeed(), 2),
            formatDouble(reading.getTrackAngle(), 2),
            formatDouble(reading.getMagneticVariation(), 2),
            formatInteger(reading.getFixQuality()),
            formatInteger(reading.getFixType()),
            formatInteger(reading.getSatelliteCount()),
            formatInteger(reading.getSatellitesInViewCount()),
            formatIntegerList(reading.getSatellitesInViewPrns()),
            formatDouble(reading.getHdop(), 2),
            formatDouble(reading.getVdop(), 2),
            formatDouble(reading.getPdop(), 2),
            formatDouble(reading.getGeoidHeight(), 2),
            formatString(reading.getUtcDate()),
            formatString(reading.getUtcTime()),
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
                    writer.write("localTimestamp,utcTimestamp,utcDate,utcTime,latitude,longitude,altitude,speed,trackAngle,magneticVariation,fixQuality,fixType,satelliteCount,satellitesInViewCount,satellitesInViewPrns,hdop,vdop,pdop,geoidHeight");
                    writer.newLine();
                }

                String csvLine = String.format(
                    "%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    reading.getLocalTimestamp(),
                    csvValue(reading.getUtcTimestamp()),
                    csvValue(reading.getUtcDate()),
                    csvValue(reading.getUtcTime()),
                    csvValue(reading.getLatitude()),
                    csvValue(reading.getLongitude()),
                    csvValue(reading.getAltitude()),
                    csvValue(reading.getSpeed()),
                    csvValue(reading.getTrackAngle()),
                    csvValue(reading.getMagneticVariation()),
                    csvValue(reading.getFixQuality()),
                    csvValue(reading.getFixType()),
                    csvValue(reading.getSatelliteCount()),
                    csvValue(reading.getSatellitesInViewCount()),
                    csvListValue(reading.getSatellitesInViewPrns()),
                    csvValue(reading.getHdop()),
                    csvValue(reading.getVdop()),
                    csvValue(reading.getPdop()),
                    csvValue(reading.getGeoidHeight())
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

    private String csvListValue(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner("|");
        for (Integer value : values) {
            if (value != null) {
                joiner.add(String.valueOf(value));
            }
        }
        return joiner.toString();
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
            if (!candidate.openPort()) {
                continue;
            }

            boolean isGpsPort;
            try {
                isGpsPort = probePortForSupportedGpsSentences(candidate);
            } finally {
                if (candidate.isOpen()) {
                    candidate.closePort();
                }
            }

            if (!isGpsPort) {
                System.err.println("[DRONE GPS] Auto-scan rejected " + path + " (no supported NMEA sentences)");
                continue;
            }

            SerialPort confirmed = SerialPort.getCommPort(path);
            configure(confirmed, baudRate);
            if (confirmed.openPort()) {
                activePortPath = path;
                System.out.println("[DRONE GPS] Auto-scan selected " + path + " (NMEA verified)");
                return confirmed;
            }
        }
        return null;
    }

    private boolean probePortForSupportedGpsSentences(SerialPort port) {
        long deadlineMs = System.currentTimeMillis() + AUTO_SCAN_PROBE_WINDOW_MS;
        int supportedSentenceCount = 0;

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(port.getInputStream(), StandardCharsets.US_ASCII))) {

            while (running.get() && port.isOpen() && System.currentTimeMillis() < deadlineMs) {
                String line = reader.readLine();
                if (line == null || line.isBlank()) {
                    continue;
                }

                for (String sentence : splitIntoSentenceCandidates(line)) {
                    if (isSupportedGpsSentence(sentence)) {
                        supportedSentenceCount++;
                        if (supportedSentenceCount >= AUTO_SCAN_REQUIRED_SUPPORTED_SENTENCES) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException exception) {
            return false;
        }

        return false;
    }

    private boolean isSupportedGpsSentence(String sentence) {
        String sentenceType = extractSentenceType(sentence);
        if (sentenceType == null) {
            return false;
        }
        return isRmcSentence(sentenceType)
            || isGgaSentence(sentenceType)
            || isGsaSentence(sentenceType)
            || isGsvSentence(sentenceType);
    }

    private String extractSentenceType(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return null;
        }

        String sanitized = sanitizeSentence(sentence);
        int start = sanitized.indexOf('$');
        if (start < 0) {
            return null;
        }
        if (start > 0) {
            sanitized = sanitized.substring(start);
        }
        if (sanitized.isBlank() || !sanitized.startsWith("$")) {
            return null;
        }
        if (!isChecksumValid(sanitized)) {
            return null;
        }

        int checksumIndex = sanitized.indexOf('*');
        String payload = checksumIndex > 0 ? sanitized.substring(0, checksumIndex) : sanitized;
        String[] fields = payload.split(",", -1);
        if (fields.length == 0) {
            return null;
        }

        String sentenceType = sanitizeField(fields[0]).toUpperCase();
        if (sentenceType.length() < 6 || !sentenceType.startsWith("$")) {
            return null;
        }
        return sentenceType;
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

    private boolean isGsaSentence(String sentenceType) {
        return sentenceType != null && sentenceType.startsWith("$") && sentenceType.endsWith("GSA");
    }

    private boolean isGsvSentence(String sentenceType) {
        return sentenceType != null && sentenceType.startsWith("$") && sentenceType.endsWith("GSV");
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
            -1.0,
            -1.0,
            -1,
            -1,
            -1,
            -1,
            List.of(),
            -1.0,
            -1.0,
            -1.0,
            -1.0,
            utcStatus,
            utcStatus,
            utcStatus,
            System.currentTimeMillis()
        );
        latestReading.set(placeholder);

        System.out.println(
            "[DRONE GPS] lat=-1 lon=-1 alt(m)=-1 speed(knots)=-1 track=-1 magVar=-1 fixQ=-1 fixType=-1 satsUsed=-1 satsView=-1 satPrns=[] hdop=-1 vdop=-1 pdop=-1 geoid=-1 utcDate="
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

    private String formatIntegerList(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.toString();
    }

    private String normalizeUtcTime(String rawTime) {
        if (rawTime == null) {
            return null;
        }
        String cleaned = rawTime.replaceAll("[^0-9.]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizeRmcDate(String rawDate) {
        String cleaned = extractNumeric(rawDate);
        if (cleaned == null || cleaned.length() < 6) {
            return null;
        }
        return cleaned.substring(0, 6);
    }
}
