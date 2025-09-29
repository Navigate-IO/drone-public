package com.drone.data;

import com.google.gson.annotations.SerializedName;

public class SensorValue {
    @SerializedName(value="ts", alternate={"rts"})
    private long ts;//: 20, // relative time stamp
    private double t;//: 52, // temperature in degrees F, integer
    private double c;//: 80, // ESP32 chip temperature F
    private double h;//: 60, // relative humidity, percent, integer
    private double l;//: 430, // light level, lumens, integer
    private double p;//: 995, // pressure, hPa, integer
    private int x;//: 3, // accelerometer X, mg, integer
    private int y;//: 4, // accelerometer Y mg, integer
    private int z;//: 5,

    public SensorValue() {
    }

    public long getTs() {
        return ts;
    }

    public void setRts(long ts) {
        this.ts = ts;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public double getL() {
        return l;
    }

    public void setL(double l) {
        this.l = l;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }
}
