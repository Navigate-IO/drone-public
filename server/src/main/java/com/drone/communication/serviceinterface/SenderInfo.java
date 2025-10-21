package com.drone.communication.serviceinterface;

public class SenderInfo {
    private String id;
    private String senderUrlPath;

    public SenderInfo(String id, String senderUrlPath) {
        this.id = id;
        this.senderUrlPath = senderUrlPath;
    }
    public SenderInfo(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderUrlPath() {
        return senderUrlPath;
    }

    public void setSenderUrlPath(String senderUrlPath) {
        this.senderUrlPath = senderUrlPath;
    }
}
