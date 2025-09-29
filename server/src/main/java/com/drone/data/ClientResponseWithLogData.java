package com.drone.data;

public class ClientResponseWithLogData extends ClientCoreData {
    private ClientLogData ldat;
    public ClientResponseWithLogData() {
        super();
    }

    public ClientLogData getLdat() {
        return ldat;
    }

    public void setLdat(ClientLogData ldat) {
        this.ldat = ldat;
    }
}
