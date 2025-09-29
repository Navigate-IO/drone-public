package com.drone.data;

public class ClientCoreData {
    private String ccmd; // client command
    private int fox; // FoxNode ID number
    private String fip; //: "192.168.60.32", // FoxNode Sensor Client fixed IP
    private long fts; // 1737235233, // FoxNode current time stamp
    private String fbv;// 4.92, // FoxNode battery voltage
    private int fcnt;//123, // Number of sensor sample in the client
    private long ftso;// 1737235000, // 'o'ldest timestamp of sensor values
    private long ftsr;//: 1737235180, // most 'r'ecnt time stamp of sensor values
    private int wrxs;//: -30, //'w'ifi receive ('rx') strength in dBm
    private double lat;//: 36.08914, // latitude
    private double lon;//: -79.16151, // longitude
    private int elev;//: 186, // MSL elevation in meters
//    private SensorData data;
//    private ClientLogData ldat;

    public ClientCoreData() {
//        this.ccmd = "hi";
//        this.fox = 1;
//        this.fip = "192.168.60.32";
//        this.fts = 1737235233;
//        this.fbv = "4.92";
//        this.fcnt = 123;
//        this.ftso = 1737235000;
//        this.ftsr = 1737235180;
//        this.wrxs = -30;
//        this.lat = 36.08914;
//        this.lon = -79.16151;
//        this.elev = 186;
    }

    public String getCcmd() {
        return ccmd;
    }

    public void setCcmd(String ccmd) {
        this.ccmd = ccmd;
    }

    public int getFox() {
        return fox;
    }

    public void setFox(int fox) {
        this.fox = fox;
    }

    public String getFip() {
        return fip;
    }

    public void setFip(String fip) {
        this.fip = fip;
    }

    public long getFts() {
        return fts;
    }

    public void setFts(long fts) {
        this.fts = fts;
    }

    public String getFbv() {
        return fbv;
    }

    public void setFbv(String fbv) {
        this.fbv = fbv;
    }

    public int getFcnt() {
        return fcnt;
    }

    public void setFcnt(int fcnt) {
        this.fcnt = fcnt;
    }

    public long getFtso() {
        return ftso;
    }

    public void setFtso(long ftso) {
        this.ftso = ftso;
    }

    public long getFtsr() {
        return ftsr;
    }

    public void setFtsr(long ftsr) {
        this.ftsr = ftsr;
    }

    public int getWrxs() {
        return wrxs;
    }

    public void setWrxs(int wrxs) {
        this.wrxs = wrxs;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lng) {
        this.lon = lng;
    }

    public int getElev() {
        return elev;
    }

    public void setElev(int elev) {
        this.elev = elev;
    }

//    public SensorData getData() {
//        return data;
//    }
//
//    public void setData(SensorData data) {
//        this.data = data;
//    }

//    public ClientLogData getLdat() {
//        return ldat;
//    }
//
//    public void setLdat(ClientLogData ldat) {
//        this.ldat = ldat;
//    }
}
