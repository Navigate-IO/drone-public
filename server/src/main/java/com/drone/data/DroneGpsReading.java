package com.drone.data;

import java.util.List;

public class DroneGpsReading {
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
    private Double trackAngle;
    private Double magneticVariation;
    private Integer fixQuality;
    private Integer fixType;
    private Integer satelliteCount;
    private Integer satellitesInViewCount;
    private List<Integer> satellitesInViewPrns;
    private Double hdop;
    private Double vdop;
    private Double pdop;
    private Double geoidHeight;
    private String utcDate;
    private String utcTime;
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

    public DroneGpsReading(Double latitude, Double longitude, Double altitude,
                           Double speed, Double trackAngle, Double magneticVariation,
                           Integer fixQuality, Integer fixType, Integer satelliteCount,
                           Integer satellitesInViewCount, List<Integer> satellitesInViewPrns,
                           Double hdop, Double vdop, Double pdop, Double geoidHeight,
                           String utcDate, String utcTime, String utcTimestamp, long localTimestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.trackAngle = trackAngle;
        this.magneticVariation = magneticVariation;
        this.fixQuality = fixQuality;
        this.fixType = fixType;
        this.satelliteCount = satelliteCount;
        this.satellitesInViewCount = satellitesInViewCount;
        this.satellitesInViewPrns = satellitesInViewPrns;
        this.hdop = hdop;
        this.vdop = vdop;
        this.pdop = pdop;
        this.geoidHeight = geoidHeight;
        this.utcDate = utcDate;
        this.utcTime = utcTime;
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

    public Double getTrackAngle() {
        return trackAngle;
    }

    public void setTrackAngle(Double trackAngle) {
        this.trackAngle = trackAngle;
    }

    public Double getMagneticVariation() {
        return magneticVariation;
    }

    public void setMagneticVariation(Double magneticVariation) {
        this.magneticVariation = magneticVariation;
    }

    public Integer getFixQuality() {
        return fixQuality;
    }

    public void setFixQuality(Integer fixQuality) {
        this.fixQuality = fixQuality;
    }

    public Integer getFixType() {
        return fixType;
    }

    public void setFixType(Integer fixType) {
        this.fixType = fixType;
    }

    public Integer getSatelliteCount() {
        return satelliteCount;
    }

    public void setSatelliteCount(Integer satelliteCount) {
        this.satelliteCount = satelliteCount;
    }

    public Integer getSatellitesInViewCount() {
        return satellitesInViewCount;
    }

    public void setSatellitesInViewCount(Integer satellitesInViewCount) {
        this.satellitesInViewCount = satellitesInViewCount;
    }

    public List<Integer> getSatellitesInViewPrns() {
        return satellitesInViewPrns;
    }

    public void setSatellitesInViewPrns(List<Integer> satellitesInViewPrns) {
        this.satellitesInViewPrns = satellitesInViewPrns;
    }

    public Double getHdop() {
        return hdop;
    }

    public void setHdop(Double hdop) {
        this.hdop = hdop;
    }

    public Double getVdop() {
        return vdop;
    }

    public void setVdop(Double vdop) {
        this.vdop = vdop;
    }

    public Double getPdop() {
        return pdop;
    }

    public void setPdop(Double pdop) {
        this.pdop = pdop;
    }

    public Double getGeoidHeight() {
        return geoidHeight;
    }

    public void setGeoidHeight(Double geoidHeight) {
        this.geoidHeight = geoidHeight;
    }

    public String getUtcDate() {
        return utcDate;
    }

    public void setUtcDate(String utcDate) {
        this.utcDate = utcDate;
    }

    public String getUtcTime() {
        return utcTime;
    }

    public void setUtcTime(String utcTime) {
        this.utcTime = utcTime;
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
