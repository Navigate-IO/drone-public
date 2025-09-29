package com.drone.data;

import java.util.ArrayList;

public class SensorData {
    private int fox;//: 12, // the FoxNode ID number
    private int dlen;//: 42, // number of readings in this dump
    private long ftso;//: 1737235000, // base timestamp
    private ArrayList<SensorValue> dump;

    public SensorData() {
//        this.fox = 12;
//        this.dlen = 42;
//        this.ftso = 1737235000;
//        dump = new ArrayList<>();
    }

    public int getFox() {
        return fox;
    }

    public void setFox(int fox) {
        this.fox = fox;
    }

    public int getDlen() {
        return dlen;
    }

    public void setDlen(int dlen) {
        this.dlen = dlen;
    }

    public long getFtso() {
        return ftso;
    }

    public void setFtso(long ftso) {
        this.ftso = ftso;
    }

    public ArrayList<SensorValue> getDump() {
        return dump;
    }

    public void setDump(ArrayList<SensorValue> dump) {
        this.dump = dump;
    }
}
