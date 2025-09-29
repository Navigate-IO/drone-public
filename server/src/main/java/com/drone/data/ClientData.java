package com.drone.data;

public class ClientData {
    private ClientCoreData core;
    private SensorData data;
    public ClientData() {

    }

    public ClientCoreData getCore() {
        return core;
    }

    public void setCore(ClientCoreData core) {
        this.core = core;
    }

    public SensorData getData() {
        return data;
    }

    public void setData(SensorData data) {
        this.data = data;
    }
}
