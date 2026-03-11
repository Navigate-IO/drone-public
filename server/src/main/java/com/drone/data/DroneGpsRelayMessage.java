package com.drone.data;

public class DroneGpsRelayMessage {
    private String messageType;
    private DroneGpsReading reading;

    public DroneGpsRelayMessage() {
    }

    public DroneGpsRelayMessage(String messageType, DroneGpsReading reading) {
        this.messageType = messageType;
        this.reading = reading;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public DroneGpsReading getReading() {
        return reading;
    }

    public void setReading(DroneGpsReading reading) {
        this.reading = reading;
    }
}
