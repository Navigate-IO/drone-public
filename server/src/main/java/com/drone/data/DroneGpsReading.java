package com.drone.data;

public class DroneGpsReading {
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
    private Integer fixQuality;
    private Integer satelliteCount;
    private String utcTimestamp;
    private long localTimestamp;

    public DroneGpsReading() {
    }

    public DroneGpsReading(Double latitude, Double longitude, Double altitude,
                           Double speed, Integer fixQuality, Integer satelliteCount,
                           String utcTimestamp, long localTimestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.fixQuality = fixQuality;
        this.satelliteCount = satelliteCount;
        this.utcTimestamp = utcTimestamp;
        this.localTimestamp = localTimestamp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Integer getFixQuality() {
        return fixQuality;
    }

    public void setFixQuality(Integer fixQuality) {
        this.fixQuality = fixQuality;
    }

    public Integer getSatelliteCount() {
        return satelliteCount;
    }

    public void setSatelliteCount(Integer satelliteCount) {
        this.satelliteCount = satelliteCount;
    }

    public String getUtcTimestamp() {
        return utcTimestamp;
    }

    public void setUtcTimestamp(String utcTimestamp) {
        this.utcTimestamp = utcTimestamp;
    }

    public long getLocalTimestamp() {
        return localTimestamp;
    }

    public void setLocalTimestamp(long localTimestamp) {
        this.localTimestamp = localTimestamp;
    }
}
