package com.drone.gps;

import com.drone.data.DroneGpsReading;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroneGpsSerialReaderParsingTest {

    @Test
    void shouldParseMixedNmeaChunkWithRmcAndGga() throws Exception {
        String sample = "$GLGSA,A,3,65,66,81,72,88,,,,,,,,1.55,1.30,0.84*1E "
            + "$GNRMC,053301.000,A,3856.6996,N,07724.2872,W,0.51,26.04,100126,,,A*57 "
            + "$GNVTG,26.04,T,,M,0.51,N,0.95,K,A*1B "
            + "$GNGGA,053302.000,3856.6996,N,07724.2873,W,1,08,1.30,89.6,M,-33.4,M,,*73";

        DroneGpsSerialReader reader = DroneGpsSerialReader.getInstance();
        Method method = DroneGpsSerialReader.class.getDeclaredMethod("handleIncomingText", String.class);
        method.setAccessible(true);
        method.invoke(reader, sample);

        DroneGpsReading reading = reader.getLatestReading();
        assertNotNull(reading);
        assertNotNull(reading.getLatitude());
        assertNotNull(reading.getLongitude());
        assertNotNull(reading.getAltitude());
        assertNotNull(reading.getSpeed());
        assertNotNull(reading.getFixQuality());
        assertNotNull(reading.getSatelliteCount());

        assertTrue(reading.getLatitude() > 38.0 && reading.getLatitude() < 39.0);
        assertTrue(reading.getLongitude() < -77.0 && reading.getLongitude() > -78.0);
        assertTrue(reading.getAltitude() > 80.0 && reading.getAltitude() < 100.0);
        assertTrue(reading.getSpeed() >= 0.0);
        assertTrue(reading.getFixQuality() >= 1);
        assertTrue(reading.getSatelliteCount() >= 1);
    }
}
