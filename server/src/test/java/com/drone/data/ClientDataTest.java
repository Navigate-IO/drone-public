package com.drone.data;

import com.drone.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClientDataTest {
    private static final String jsonString = "{\"core\":{\"ccmd\":\"rval\",\"fox\":10,\"fip\":\"192.168.40.40\",\"fts\":946755453,\"fbv\":\"F\",\"fcnt\":2,\"ftso\":946755395,\"ftsr\":946755425,\"wrxs\":-23,\"lat\":64.82,\"lon\":-147.72,\"elev\":140},\"data\":{\"fox\":10,\"dlen\":2,\"ftso\":946755395,\"dump\":[{\"rts\":946755395,\"t\":25.2195,\"c\":33.9,\"h\":39.52697,\"l\":13.02,\"p\":1010.288,\"x\":-77,\"y\":-4,\"z\":999},{\"rts\":946755425,\"t\":25.16915,\"c\":37.9,\"h\":39.49645,\"l\":13.34,\"p\":1010.323,\"x\":-53,\"y\":-15,\"z\":1020}]}}";
    private static final String jsonStringNew = "{\"core\":{\"ccmd\":\"rval\",\"fox\":10,\"fip\":\"192.168.40.40\",\"fts\":946755453,\"fbv\":\"F\",\"fcnt\":2,\"ftso\":946755395,\"ftsr\":946755425,\"wrxs\":-23,\"lat\":64.82,\"lon\":-147.72,\"elev\":140},\"data\":{\"fox\":10,\"dlen\":2,\"ftso\":946755395,\"dump\":[{\"ts\":946755395,\"t\":25.2195,\"c\":33.9,\"h\":39.52697,\"l\":13.02,\"p\":1010.288,\"x\":-77,\"y\":-4,\"z\":999},{\"ts\":946755425,\"t\":25.16915,\"c\":37.9,\"h\":39.49645,\"l\":13.34,\"p\":1010.323,\"x\":-53,\"y\":-15,\"z\":1020}]}}";

    @Test
    public void test() {
        ClientData clientData = Utils.fromJson(jsonString, ClientData.class);
        ClientData clientDataNew = Utils.fromJson(jsonStringNew, ClientData.class);
        String json = Utils.toJson(clientData);
        String jsonNew = Utils.toJson(clientDataNew);
        SensorData sensorData = Utils.fromJson(jsonString, SensorData.class);
        SensorData sensorDataNew = Utils.fromJson(jsonStringNew, SensorData.class);
        String sensorDataJson = Utils.toJson(sensorData);
        String sensorDataJsonNew = Utils.toJson(sensorDataNew);
        System.out.println("pretty json=" + json);
        System.out.println("original json=" + jsonString);
        System.out.println("pretty sensor data json=" + sensorDataJson);
        System.out.println("new pretty json=" + jsonNew);
        System.out.println("new original json=" + jsonStringNew);
        System.out.println("new pretty sensor data json=" + sensorDataJsonNew);
        Assertions.assertTrue(clientData.getData().getDlen() == 2);
        Assertions.assertTrue(clientData.getData().getDump().get(0).getTs() == 946755395);
        Assertions.assertTrue(clientDataNew.getData().getDump().get(0).getTs() == 946755395);
    }
}
