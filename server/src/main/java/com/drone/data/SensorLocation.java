package com.drone.data;

public class SensorLocation {
    private int fox;
    private double lat;
    private double lng;
    private double elev;

    public SensorLocation() {

    }

    public int getFox() {
        return fox;
    }

    public void setFox(int fox) {
        this.fox = fox;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getElev() {
        return elev;
    }

    public void setElev(double elev) {
        this.elev = elev;
    }
}
