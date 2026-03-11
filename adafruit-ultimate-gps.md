# Adafruit Ultimate GPS

**Created by lady ada**
**Source:** <https://learn.adafruit.com/adafruit-ultimate-gps>

---

## Overview

The Adafruit Ultimate GPS breakout board is built around the **MTK3339 chipset**, a high-quality GPS module that can track up to 22 satellites on 66 channels.

### Key Features

- **-165 dBm** sensitivity, **10 Hz** updates, 66 channels
- 5V friendly design, only **20mA** current draw
- Breadboard friendly + two mounting holes
- RTC battery-compatible
- Built-in datalogging
- PPS output on fix
- Internal patch antenna + u.FL connector for external active antenna
- Fix status LED

### Two Versions

There are two versions of the breakout board:

1. **Serial Breakout (#746)** — wires to projects via serial lines, read from a microcontroller.
2. **USB Board (#4279)** — connects via USB cable to a computer. Not programmable in Arduino.

---

## Specifications

### Module Specs

| Parameter | Value |
|---|---|
| Satellites | 22 tracking, 66 searching |
| Patch Antenna Size | 15mm × 15mm × 4mm |
| Update Rate | 1 to 10 Hz |
| Position Accuracy | 1.8 meters |
| Velocity Accuracy | 0.1 meters/s |
| Warm/Cold Start | 34 seconds |
| Acquisition Sensitivity | -145 dBm |
| Tracking Sensitivity | -165 dBm |
| Maximum Velocity | 515 m/s |
| Vin Range | 3.0–5.5 VDC |
| Operating Current | 25mA tracking, 20mA navigation |
| Output | NMEA 0183, 9600 baud default |
| DGPS/WAAS/EGNOS | Supported |
| PRN Channels | Up to 210 |

### Breakout Board Details

| Parameter | Value |
|---|---|
| Weight | 8.5g (without coin cell/holder) |
| Dimensions | 25.5mm × 35mm × 6.5mm (1.0" × 1.35" × 0.25") |

---

## Pinouts

### Breakout Power Pins

**Required:**

- **VIN** — Power input, connect to 3–5 VDC. Use a clean, quiet power supply (LDO preferred over switching supply).
- **GND** — Power and signal ground.

**Optional:**

- **VBAT** — Input pin connected to the GPS real-time clock battery backup. Use the battery spot on the back, or connect an external battery under 3.3V. For V1/V2 modules: cut the trace on the back between the RTC solder pads first.
- **EN** — Enable pin, pulled high with a 10K resistor. Pull to ground to turn off the GPS module. Useful for low-power projects. You will lose your fix when disabled.
- **3.3V** — Output from the onboard 3.3V regulator. Can provide at least 100mA.

### Breakout Serial Data Pins

- **TX** — Transmits data from the GPS module to your microcontroller/computer. 3.3V logic level. Default 9600 baud.
- **RX** — Send data to the GPS. Accepts 3.3V or 5V logic (has a level shifter). Default 9600 baud. Requires checksum'd NMEA sentences.

### Other Pins

- **FIX** — Output pin, same as the red LED driver. Pulses once/second with no fix. When fix is acquired, stays low with a 200ms high pulse every 15 seconds.
- **PPS** — Pulse Per Second output (V3 modules only). Low most of the time, pulses high (3.3V) once per second for 50–100ms.

---

## Versions

| Version | Module | Features |
|---|---|---|
| V1 | PA6B (MT3329) | No built-in datalogging |
| V2 | PA6C (MTK3339) | Datalogging support |
| V3 | PA6H (MTK3339) | PPS output + external antenna support |

If silkscreen says "MTK3329" → V1 (PA6B). If "v3" next to the name → V3 (PA6H).

---

## Battery Backup

The GPS has a built-in RTC that keeps time even without power or a fix. Attach a **CR1220 coin cell** to the battery holder on the back.

- Backup circuit draws **7 µA**, so a CR1220 lasts ~240 days continuously as backup.
- A backup battery preserves baud rate and configuration settings across power cycles.
- All date/time data is in **UTC** — you must convert to local time zone in software.

**V1/V2 modules:** Cut the trace between the two solder pads labeled "RTC" on the back before inserting a battery.

**V3 modules:** No trace to cut — they have a built-in diode.

---

## External Antenna

External antenna support is available on **V3 modules only** (requires the u.FL connector).

- The built-in patch antenna provides -165 dBm sensitivity.
- For enclosures, metal shields, or more sensitivity, attach a **3V active GPS antenna** via the u.FL connector.
- The module **auto-detects** the external antenna and switches over — no commands needed.
- Most GPS antennas use SMA connectors; use a **u.FL to SMA adapter cable**.

### Antenna Status

The sentence `$PGTOP,11,x` reports antenna status:

| x value | Meaning |
|---|---|
| 3 | Using external antenna |
| 2 | Using internal antenna |
| 1 | Antenna short or problem |

On newer modules, enable this report with: `gps.sendCommand(PGCMD_ANTENNA)`

> **Note:** u.FL connectors are small and delicate. Use strain relief once an adapter is attached to avoid ripping off the connector.

---

## Direct Computer Wiring

To test the GPS directly via a computer using an Arduino as a USB-to-serial bridge:

1. Upload a blank sketch to the Arduino:

```cpp
// Bypass the ATmega chip — connect GPS directly to USB/Serial converter.
// Connect VIN to +5V
// Connect GND to Ground
// Connect GPS RX to Digital 0
// Connect GPS TX to Digital 1
void setup() {}
void loop() {}
```

2. Wire GPS TX → Arduino Digital 1, GPS RX → Arduino Digital 0.

> **Note:** TX→TX and RX→RX is correct here because you're bypassing the Arduino chip and using its USB-serial converter directly. This only works with Arduino UNO compatibles (not native USB boards).

3. Open Serial Monitor at **9600 baud**.

### NMEA Sentences

The GPS outputs raw **NMEA sentences**. The most common are:

- **$GPRMC** — Recommended Minimum Coordinates: time, date, lat, lon, speed, heading.
- **$GPGGA** — Fix data including altitude.

Example RMC sentence:
```
$GPRMC,194509.000,A,4042.6142,N,07400.4168,W,2.03,221.11,160412,,,A*77
```

| Field | Value | Meaning |
|---|---|---|
| 194509.000 | Time | 19:45:09 UTC |
| A | Status | A = Active (valid fix), V = Void |
| 4042.6142,N | Latitude | 40° 42.6142' N |
| 07400.4168,W | Longitude | 74° 00.4168' W |
| 2.03 | Speed | 2.03 knots |
| 221.11 | Track angle | Heading in degrees |
| 160412 | Date | 16 April 2012 |
| *77 | Checksum | Data integrity check |

### Important: Coordinate Format

> The geolocation data is **NOT in decimal degrees**. It uses degrees and decimal minutes:
> - Latitude: `DDMM.MMMM` (first two chars are degrees)
> - Longitude: `DDDMM.MMMM` (first three chars are degrees)

To view in Google Maps, convert to: `+40 42.6142', -74 00.4168'` (N/E = positive, S/W = negative).

> **GPS modules always send data even without a fix.** For valid data, the antenna must point up with a clear sky view. A fix can take under 45 seconds in ideal conditions, or up to 30+ minutes depending on environment.

---

## Breakout Arduino Wiring

### Software Serial Boards (Arduino UNO, etc.)

Uses `SoftwareSerial` with pin 8 (RX) and pin 7 (TX):

```cpp
SoftwareSerial mySerial(8, 7);
```

Wiring:
- **VIN** → +5V
- **GND** → Ground
- **GPS RX** → Arduino Digital 7
- **GPS TX** → Arduino Digital 8

> Note: Arduino TX connects to GPS RX, and Arduino RX connects to GPS TX (normal cross-wiring).

### Hardware Serial Boards (Leonardo, M0, M4, ESP32, etc.)

Uses `Serial1`. Wire:
- **VIN** → 3.3V (match board logic level)
- **GND** → Ground
- **GPS RX** → Board TX
- **GPS TX** → Board RX

### Installing the Library

1. Open Arduino Library Manager.
2. Search for **Adafruit GPS** and click Install.

### Example: Echo Test

Load `SoftwareSerial_echotest` or `GPS_HardwareSerial_EchoTest` from the library examples.

Open Serial Monitor at **115200 baud**.

### Configuring Output

In `setup()`, configure sentences and update rate:

```cpp
// RMC + GGA (recommended)
GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);

// RMC only (for high update rates)
// GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCONLY);

// All data (use 1 Hz at 9600 baud)
// GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_ALLDATA);

// Update rates
// GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);
GPS.sendCommand(PMTK_SET_NMEA_UPDATE_5HZ);
// GPS.sendCommand(PMTK_SET_NMEA_UPDATE_10HZ);
```

> 10 Hz is the max speed. At 9600 baud, use RMC only for 10 Hz. Most projects only need RMC + GGA at 1 Hz.

---

## Breakout Arduino Parsing

Load `GPS_HardwareSerial_Parsing` or `GPS_SoftwareSerial_Parsing` from the library examples.

### How It Works

1. Call `GPS.read()` constantly in the main loop.
2. Check `GPS.newNMEAreceived()` — if true, call `GPS.parse(GPS.lastNMEA())`.
3. Access parsed data:

| Property | Description |
|---|---|
| `GPS.fix` | 1 if fix acquired, 0 if not |
| `GPS.day`, `GPS.month`, `GPS.year` | Current date |
| `GPS.latitude`, `GPS.longitude` | Position |
| `GPS.speed` | Speed in knots |
| `GPS.angle` | Track angle |
| `GPS.altitude` | Altitude in centimeters |
| `GPS.satellites` | Number of satellites |

> You need a fix for valid time/location data. The antenna must be outside or near a window pointing up.

---

## CircuitPython & Python Setup

### CircuitPython Microcontroller Wiring

Example with Metro M0 Express:

- Board **5V** or **3.3V** → GPS **VIN**
- Board **GND** → GPS **GND**
- Board serial **TX** → GPS **RX**
- Board serial **RX** → GPS **TX**

### Python / Raspberry Pi Wiring

**Option A — USB-to-serial converter:**

- GPS Vin → USB 5V or 3V (red wire)
- GPS Ground → USB Ground (black wire)
- GPS RX → USB TX (green wire)
- GPS TX → USB RX (white wire)

**Option B — Pi's built-in UART:**

- GPS Vin → 3.3V
- GPS Ground → Ground
- GPS RX → Pi TX
- GPS TX → Pi RX

> For the built-in UART, disable the serial console and enable serial port hardware in `raspi-config`.

**Option C — Ultimate GPS USB:** Plug USB-C or Micro-B cable directly from computer to the GPS USB board.

### Installing Libraries

**CircuitPython:**
```
# Copy adafruit_gps.mpy to your board's lib folder
```

**Python (pip):**
```bash
sudo pip3 install adafruit-circuitpython-gps
```

---

## CircuitPython & Python UART Usage

### Example Parsing Code

```python
import time
import board
import busio
import adafruit_gps

# Serial connection
rx = board.RX
tx = board.TX
uart = busio.UART(rx, tx, baudrate=9600, timeout=10)

# For computer/Raspberry Pi, use pyserial instead:
# import serial
# uart = serial.Serial("/dev/ttyUSB0", baudrate=9600, timeout=10)

# Create GPS instance
gps = adafruit_gps.GPS(uart, debug=False)

# Enable GGA and RMC output
gps.send_command(b"PMTK314,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0")

# Set 1 Hz update rate
gps.send_command(b"PMTK220,1000")

last_print = time.monotonic()
while True:
    gps.update()
    current = time.monotonic()
    if current - last_print >= 1.0:
        last_print = current
        if not gps.has_fix:
            print("Waiting for fix...")
            continue

        print("=" * 40)
        print(f"Latitude: {gps.latitude:.6f} degrees")
        print(f"Longitude: {gps.longitude:.6f} degrees")
        print(f"Fix quality: {gps.fix_quality}")

        if gps.satellites is not None:
            print(f"# satellites: {gps.satellites}")
        if gps.altitude_m is not None:
            print(f"Altitude: {gps.altitude_m} meters")
        if gps.speed_knots is not None:
            print(f"Speed: {gps.speed_knots} knots")
        if gps.track_angle_deg is not None:
            print(f"Track angle: {gps.track_angle_deg} degrees")
```

### Key Points

- Call `gps.update()` every loop iteration (at least twice per second).
- Check `gps.has_fix` before reading location data.
- Some attributes (`altitude_m`, `satellites`, etc.) may be `None` — always check before using.

---

## CircuitPython Datalogging

### Storage Options

1. **SD card** (recommended) — more space, easy to copy data to computer.
2. **Internal filesystem** — limited space (a few KB–MB), requires special setup.

### SD Card Setup

Wire an SD card holder to SPI, install the Adafruit SD card library, and mount the card.

### Internal Filesystem Setup

Edit `boot.py` to enable filesystem writes:

```python
import digitalio
import board
import storage

switch = digitalio.DigitalInOut(board.D5)
switch.direction = digitalio.Direction.INPUT
switch.pull = digitalio.Pull.UP

# D5 connected to ground = USB drive editable (no filesystem writes)
# D5 floating = filesystem writable (no USB code editing)
storage.remount("/", not switch.value)
```

### Datalogging Code

```python
import board
import busio
import adafruit_gps

LOG_FILE = "/sd/gps.txt"  # or "/gps.txt" for internal storage
LOG_MODE = "ab"  # append mode

uart = busio.UART(board.TX, board.RX, baudrate=9600, timeout=10)
gps = adafruit_gps.GPS(uart)

with open(LOG_FILE, LOG_MODE) as outfile:
    while True:
        sentence = gps.readline()
        if not sentence:
            continue
        print(str(sentence, "ascii").strip())
        outfile.write(sentence)
        outfile.flush()
```

---

## Built-In Logging (LOCUS)

The MTK3339 has a built-in data logger using internal 64K flash memory.

### Capabilities

- Logs **date, time, latitude, longitude, altitude** every 15 seconds (when there is a fix).
- Stores up to **16 hours** of data.
- Automatically appends data — no data loss on power failure.
- Logging interval and fields are hardcoded and cannot be changed.

### Starting the Logger

```cpp
if (GPS.LOCUS_StartLogger())
    Serial.println("STARTED!");
else
    Serial.println("no response :(");
```

Use the `locus_start` example from the Adafruit GPS library.

### Checking Status

Use the `locus_status` example. Output includes:
- Log number (trace count)
- Logging mode (fix only, interval-based)
- Interval (15 seconds)
- Number of records stored
- Flash usage percentage

### Downloading Data

Use the `locus_dump` example. Copy the output (from `$PMTKLOX,0,...` to `$PMTK001,622,3*36`) and paste into the [LOCUS Parser](https://direct.adafruit.com/gps/locus_dump).

Alternatively, use the **GPS Tool** (Windows only) to connect via COM port and query/dump/delete log memory.

---

## FAQ

### High Altitude Use
Modules with firmware version 5223+ (shipped 2013+) have been tested at 40km by simulation at the factory. Check firmware with `$PMTK605*31`. However, these modules are **not specifically designed or guaranteed** for high-altitude balloon use.

### 2019 Week Rollover
All firmware from `20110922_GTOP_EVK01_A2.10` and higher work fine through 2019. They do not pass the 2038 rollover test.

### Garbled NMEA Data
SoftwareSerial is "bitbang" UART and can choke if too many `delay()` calls are added. Consider using HardwareSerial on boards that support it (Leonardo, Mega, M0, etc.).

### Location Is Wrong ("5 Miles Off")
The coordinate format is **degrees + decimal minutes**, NOT decimal degrees. Parse accordingly:
- Latitude: `DDMM.MMMM`
- Longitude: `DDDMM.MMMM`

### Can't Get 10 Hz Output
The default 9600 baud can only handle RMC messages at 10 Hz. For more data, increase the GPS baud rate (e.g., 57600) or reduce the update rate.

### RTC Not Writable
The GPS RTC is not writable from Arduino. Once the battery is installed and the GPS gets its first satellite fix, the RTC is set automatically. Time is always in UTC.

### PPS Not Working
PPS only outputs after a **3D fix** (not just 2D). Check the `$GPGSA` sentence — the second value must be `3`.

### PPS on Ultimate GPS USB
The PPS line is tied to the serial port **RI (Ring Indicator)** pin. Read it with pyserial:

```python
import serial
ser = serial.Serial('/dev/ttyS34')
last_ri = ser.ri
while True:
    if ser.ri != last_ri:
        last_ri = ser.ri
        if last_ri:
            print("Pulse high")
        else:
            print("Pulse low")
```

---

## Resources

### Datasheets

- [MTK3329/MTK3339 command set](https://adafru.it/e7A) — fix rate, baud rate, sentence outputs
- [PA6B (MTK3329) datasheet](https://adafru.it/s0B) — V1 module
- [PA6C (MTK3339) datasheet](https://adafru.it/s0C) — V2 module
- [PA6H (MTK3339) datasheet](https://adafru.it/ria) — V3 module
- [PA1616S (MTK3339) datasheet](http://adafru.it/746161603) — V3.1 module
- [LOCUS datalogging user guide](https://adafru.it/uoc)

### Tools

- [LOCUS Parser (web)](https://adafru.it/cFg)
- [MT3339 GPS PC Tool (Windows)](https://adafru.it/uoD) — [Manual](https://adafru.it/uoE)
- [Mini GPS Tool (Windows)](https://adafru.it/rid)

### Libraries

- [Adafruit GPS Library for Arduino](https://github.com/adafruit/Adafruit-GPS-Library/)
- [Adafruit CircuitPython GPS](https://adafru.it/BuR)

### Design Files

- [EagleCAD PCB files on GitHub](https://adafru.it/s0D)
- [Fritzing object](https://adafru.it/aP3)

### Learning Resources

- [Trimble GPS Tutorial](https://adafru.it/emh)
- [Garmin GPS Tutorial](https://adafru.it/aMv)
- [NMEA Sentence Reference](https://adafru.it/kMb)
