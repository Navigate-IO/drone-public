package com.drone;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class Config {
    private String ipAddress;
    private int portNumber;
    private long takeOffTime = -1;
    private String teamName;
    private String actualIpAddress;
    private String otherDronesUrls;
    private String droneGpsSerialDevice;
    private int droneGpsBaudRate;
    private boolean droneGpsAutoPortScan;
    private int droneGpsPublishIntervalSeconds;
    private static Config instance;
    public Config()
    {
        ipAddress = "0.0.0.0";
        portNumber = 80;
        teamName = "NavigateIO";
        actualIpAddress = "192.168.40.20";
        otherDronesUrls = "";
        droneGpsSerialDevice = "/dev/ttyUSB0";
        droneGpsBaudRate = 9600;
        droneGpsAutoPortScan = false;
        droneGpsPublishIntervalSeconds = 60;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public long getTakeOffTime() {
        if (takeOffTime < 0) {
            takeOffTime = System.currentTimeMillis();
        }
        return takeOffTime;
    }

    public void setTakeOffTime(long takeOffTime) {
        this.takeOffTime = takeOffTime;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getActualIpAddress() {
        return actualIpAddress;
    }

    public void setActualIpAddress(String actualIpAddress) {
        this.actualIpAddress = actualIpAddress;
    }

    public static Config getInstance() {
        return instance;
    }

    public String getOtherDronesUrls() {
        return otherDronesUrls;
    }

    public void setOtherDronesUrls(String otherDronesUrls) {
        this.otherDronesUrls = otherDronesUrls;
    }

    public String getDroneGpsSerialDevice() {
        if (droneGpsSerialDevice == null || droneGpsSerialDevice.isBlank()) {
            return "/dev/ttyUSB0";
        }
        return droneGpsSerialDevice;
    }

    public void setDroneGpsSerialDevice(String droneGpsSerialDevice) {
        this.droneGpsSerialDevice = droneGpsSerialDevice;
    }

    public int getDroneGpsBaudRate() {
        if (droneGpsBaudRate <= 0) {
            return 9600;
        }
        return droneGpsBaudRate;
    }

    public void setDroneGpsBaudRate(int droneGpsBaudRate) {
        this.droneGpsBaudRate = droneGpsBaudRate;
    }

    public boolean isDroneGpsAutoPortScan() {
        return droneGpsAutoPortScan;
    }

    public void setDroneGpsAutoPortScan(boolean droneGpsAutoPortScan) {
        this.droneGpsAutoPortScan = droneGpsAutoPortScan;
    }

    public int getDroneGpsPublishIntervalSeconds() {
        if (droneGpsPublishIntervalSeconds <= 0) {
            return 60;
        }
        return droneGpsPublishIntervalSeconds;
    }

    public void setDroneGpsPublishIntervalSeconds(int droneGpsPublishIntervalSeconds) {
        this.droneGpsPublishIntervalSeconds = droneGpsPublishIntervalSeconds;
    }

    public static Config loadDefaultConfigFile(String configFile) {
        if (configFile == null || configFile.isBlank()) {
            configFile  = "server_config.json";
        }
        Config config;

        try {
            FileReader reader = new FileReader(new File(configFile));
            Gson gson = new Gson();
            config = gson.fromJson(reader, Config.class );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            config = new Config();
        }
        if (config.getTakeOffTime() < 0) {
            config.setTakeOffTime(System.currentTimeMillis());
        }
        instance = config;
        return config;
    }
}
