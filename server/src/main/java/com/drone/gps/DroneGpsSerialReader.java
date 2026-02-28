package com.drone.gps;

import com.drone.data.DroneGpsReading;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DroneGpsSerialReader {
    private static final String DEFAULT_SERIAL_DEVICE = "/dev/ttyUSB0";
    private static final int DEFAULT_BAUD_RATE = 9600;
    private static final long RECONNECT_DELAY_MS = 3000L;

    private static final DroneGpsSerialReader INSTANCE = new DroneGpsSerialReader();

    private final AtomicReference<DroneGpsReading> latestReading = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean placeholderPublished = new AtomicBoolean(false);
    private final Object logLock = new Object();

    private final Path logFilePath = Path.of("drone_gps_log.txt");

    private Thread workerThread;
    private volatile SerialPort activePort;

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
        if (running.get()) {
            return;
        }

        final String deviceToUse = (serialDevice == null || serialDevice.isBlank())
            ? DEFAULT_SERIAL_DEVICE : serialDevice;
        final int baudToUse = baudRate > 0 ? baudRate : DEFAULT_BAUD_RATE;

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
            SerialPort serialPort = SerialPort.getCommPort(serialDevice);
            serialPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

            if (!serialPort.openPort()) {
                System.err.println("[DRONE GPS] Serial device not available: " + serialDevice + " (retrying)");
                publishPlaceholderIfNeeded("NO_SENSOR");
                sleepQuietly(RECONNECT_DELAY_MS);
                continue;
            }

            activePort = serialPort;
            placeholderPublished.set(false);
            System.out.println("[DRONE GPS] Connected to " + serialDevice + " @ " + baudRate + " baud");

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(serialPort.getInputStream(), StandardCharsets.US_ASCII))) {

                while (running.get() && serialPort.isOpen()) {
                    String line = reader.readLine();
                    if (line == null) {
                        continue;
                    }

                    DroneGpsReading reading = processSentence(line.trim());
                    if (reading == null) {
                        continue;
                    }

                    latestReading.set(reading);
                    printReading(reading);
                    appendToLog(reading);
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
            }

            if (running.get()) {
                System.err.println("[DRONE GPS] Serial connection lost, reconnecting...");
                sleepQuietly(RECONNECT_DELAY_MS);
            }
        }
    }

    private DroneGpsReading processSentence(String sentence) {
        if (sentence == null || sentence.isBlank() || !sentence.startsWith("$")) {
            return null;
        }

        int checksumIndex = sentence.indexOf('*');
        String payload = checksumIndex > 0 ? sentence.substring(0, checksumIndex) : sentence;
        String[] fields = payload.split(",", -1);
        if (fields.length == 0) {
            return null;
        }

        String sentenceType = fields[0];
        try {
            if ("$GPRMC".equals(sentenceType) || "$GNRMC".equals(sentenceType)) {
                return parseRmc(fields);
            }
            if ("$GPGGA".equals(sentenceType) || "$GNGGA".equals(sentenceType)) {
                return parseGga(fields);
            }
        } catch (RuntimeException exception) {
            System.err.println("[DRONE GPS] Ignoring malformed sentence: " + sentence);
        }

        return null;
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
        if (rawDate == null || rawDate.length() < 6) {
            return null;
        }

        try {
            int day = Integer.parseInt(rawDate.substring(0, 2));
            int month = Integer.parseInt(rawDate.substring(2, 4));
            int year = Integer.parseInt(rawDate.substring(4, 6));
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

        String trimmed = rawTime.trim();
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
        if (rawCoordinate == null || rawCoordinate.isBlank()) {
            return null;
        }
        if (hemisphere == null || hemisphere.isBlank()) {
            return null;
        }

        try {
            double coordinate = Double.parseDouble(rawCoordinate);
            int degrees = (int) (coordinate / 100);
            double minutes = coordinate - (degrees * 100.0);
            double decimalDegrees = degrees + (minutes / 60.0);

            String normalizedHemisphere = hemisphere.trim().toUpperCase();
            if ("S".equals(normalizedHemisphere) || "W".equals(normalizedHemisphere)) {
                decimalDegrees = -decimalDegrees;
            }

            return decimalDegrees;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
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
