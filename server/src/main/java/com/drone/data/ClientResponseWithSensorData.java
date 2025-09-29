package com.drone.data;

import java.util.ArrayList;
import java.util.List;

public class ClientResponseWithSensorData extends ClientCoreData {
    private SensorData data;
    public ClientResponseWithSensorData() {
        super();
    }

    public SensorData getData() {
        return data;
    }

    public void setData(SensorData data) {
        this.data = data;
    }
}
