package com.drone.communication.serviceinterface;

public class Target {
    private String id;
    private String targetUrlPath;
    public Target() {}
    public Target(String id, String targetUrlPath) {
        this.id = id;
        this.targetUrlPath = targetUrlPath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetUrlPath() {
        return targetUrlPath;
    }

    public void setTargetUrlPath(String targetUrlPath) {
        this.targetUrlPath = targetUrlPath;
    }
}
