package com.drone.data;

public class ResponseDataObject {
    private String srep; // Server reply to Sensor Client
    private String sip;//: "192.168.60.10" // Server fixed IP
    private long sts;//: 1737235234, // Server current time stamp
    private long stsb;//: 1737235050, // beginning timestamp for values
    private long stse;//: 1737235180, // Ending timestamp for sensor values

    public
    ResponseDataObject() {
    }

    public String getSrep() {
        return srep;
    }

    public void setSrep(String srep) {
        this.srep = srep;
    }

    public String getSip() {
        return sip;
    }

    public void setSip(String sip) {
        this.sip = sip;
    }

    public long getSts() {
        return sts;
    }

    public void setSts(long sts) {
        this.sts = sts;
    }

    public long getStsb() {
        return stsb;
    }

    public void setStsb(long stsb) {
        this.stsb = stsb;
    }

    public long getStse() {
        return stse;
    }

    public void setStse(long stse) {
        this.stse = stse;
    }
}
