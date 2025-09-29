package com.drone.data;

import java.util.List;

public class CollectData {
    private int foxNodeId;
    private List<SensorValue> data;
    public CollectData() {}

    public int getFoxNodeId() {
        return foxNodeId;
    }

    public void setFoxNodeId(int foxNodeId) {
        this.foxNodeId = foxNodeId;
    }

    public List<SensorValue> getData() {
        return data;
    }

    public void setData(List<SensorValue> data) {
        this.data = data;
    }
}
