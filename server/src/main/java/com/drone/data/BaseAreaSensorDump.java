package com.drone.data;

import com.drone.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BaseAreaSensorDump {
    private String team;//  “Flying Data Miners”, // team name
    private int foundCount; //14, // # of FoxNode Sensors found
    private int sortieCount;// 8, // # of gathering flights
    private long earliestTakeOff;//: <time_stamp> // earliest take-off time
    private long lastLanding;//: <time_stamp> // last time of landing
    private long firstDumpTime;//: <time_stamp> // time of first dump
        // at take-off point
    private long lastDumpTime;//: <time_stamp> // time of last dump at
        // take-off point
    private int totalSampleCount;// 588, // Total # of samples taken
            // across all FoxNodes
    private ArrayList<CollectData> collectData = new ArrayList<>();//:
    private ArrayList<SensorLocationData> sensorLocations = new ArrayList<>();

    private static BaseAreaSensorDump baseAreaSensorDump;
    private static BaseAreaSensorDump incrementalBaseAreaSensorDump;
    private static String teamName;
    private static long takeoffTime = -1;
    private static int sampleCountToCollect = 10;

    public static synchronized BaseAreaSensorDump getInstance() {
        if (baseAreaSensorDump != null) {
           if (baseAreaSensorDump.getTotalSampleCount() > sampleCountToCollect) {
               String json = Utils.toJson(baseAreaSensorDump);
               writeToFiles(json);
               sampleCountToCollect = sampleCountToCollect + 10;
           }
            return baseAreaSensorDump;
        }

        if (takeoffTime < 0) {
            takeoffTime = System.currentTimeMillis();
            teamName = "NavigateIO";
        }
        baseAreaSensorDump = new BaseAreaSensorDump(teamName, takeoffTime);
        incrementalBaseAreaSensorDump = new BaseAreaSensorDump(teamName, takeoffTime);
        return baseAreaSensorDump;
    }

    public static synchronized void writeToFiles(String json) {
        String folder = "Output";
        File folderFile = new File(folder);
        if (!(folderFile.exists())) {
            folderFile.mkdirs();
        }
        String folderName = System.currentTimeMillis() + "";
        File subFolder = new File(folderFile, folderName);
        if (!(subFolder.exists())) {
            subFolder.mkdirs();
        }
        File file1 = new File(subFolder, "BaseAreaSensorDump.json");
        try (FileWriter fileWriter = new FileWriter(file1)) {
            //            String json1 = Utils.toJson(this.clientDataMap);
            fileWriter.write(json);
            fileWriter.flush();
        } catch (IOException e) {
//            throw new RuntimeException(e);
            e.printStackTrace();
        }
    }

    public static synchronized void initialize(String team, long time) {
        System.out.println("Intialize team name with " + team);
        System.out.println("Intialize take off time with " + time);

        teamName = team;
        takeoffTime = time;
    }

    public static synchronized void addClientData(ClientData clientData) {
        BaseAreaSensorDump temp = getInstance();
        temp.addSensorData(clientData);
        incrementalBaseAreaSensorDump.addSensorData(clientData);
    }

    public static synchronized String collectData() {
        long firstDumpTime = baseAreaSensorDump.getFirstDumpTime();
        long currentTime = System.currentTimeMillis();
        if (firstDumpTime <= 0) {
            baseAreaSensorDump.setFirstDumpTime(currentTime);
        }
        baseAreaSensorDump.setLastDumpTime(currentTime);
        baseAreaSensorDump.setLastLanding(currentTime);
        baseAreaSensorDump.setSortieCount(baseAreaSensorDump.getSortieCount() + 1);
        String result = Utils.toJson(baseAreaSensorDump);
        return result;
    }

    public static synchronized String collectIncrementData() {
        long firstDumpTime = incrementalBaseAreaSensorDump.getFirstDumpTime();
        long currentTime = System.currentTimeMillis();
        if (firstDumpTime <= 0) {
            incrementalBaseAreaSensorDump.setFirstDumpTime(currentTime);
        }
        incrementalBaseAreaSensorDump.setLastDumpTime(currentTime);
        incrementalBaseAreaSensorDump.setLastLanding(currentTime);
        incrementalBaseAreaSensorDump.setSortieCount(incrementalBaseAreaSensorDump.getSortieCount() + 1);
        String result = Utils.toJson(incrementalBaseAreaSensorDump);
        return result;
    }

    public static synchronized void resetIncrementalData() {
        incrementalBaseAreaSensorDump.getCollectData().clear();
        incrementalBaseAreaSensorDump.getSensorLocations().clear();
    }

    private BaseAreaSensorDump(String teamName, long earliestTakeOff) {
        this.team = teamName;
        this.foundCount = 0;
        this.sortieCount = 0;
        this.earliestTakeOff = earliestTakeOff;
        this.totalSampleCount = 0;
        this.firstDumpTime = -1;
    }

    private boolean contains(SensorValue value,  List<SensorValue> sensorValues) {
        for (int index=0; index < sensorValues.size(); index++) {
            SensorValue sensorValue = sensorValues.get(index);
            if (sensorValue.getTs() == value.getTs()) {
                return true;
            }
        }
        return false;
    }

    public void addSensorData(ClientData clientData) {
        int foxId = clientData.getCore().getFox();
        List<SensorValue> currentSensorDump = clientData.getData().getDump();
        for (int index = 0; index < collectData.size(); index++) {
            CollectData currentCollectData = collectData.get(index);
            if (currentCollectData.getFoxNodeId() == foxId) {
                List<SensorValue> allSensorValues = currentCollectData.getData();
                for (SensorValue sensor : currentSensorDump) {
                    if (!(contains(sensor, allSensorValues))) {
                        allSensorValues.add(sensor);
                        totalSampleCount++;
                    }
                }
                return;
            }
        }
        CollectData toBeAddedData = new CollectData();
        toBeAddedData.setFoxNodeId(foxId);
        toBeAddedData.setData(currentSensorDump);
        this.foundCount++;
        collectData.add(toBeAddedData);
        this.totalSampleCount = totalSampleCount + currentSensorDump.size();

        SensorLocationData sensorLocationData = new SensorLocationData();
        SensorLocation sensorLocation = new SensorLocation();
        sensorLocation.setFox(foxId);
        double lat = clientData.getCore().getLat();
        double lon = clientData.getCore().getLon();
        double elev = clientData.getCore().getElev();
        sensorLocation.setLat(lat);
        sensorLocation.setLng(lon);
        sensorLocation.setElev(elev);
        sensorLocationData.setSensorLoc(sensorLocation);
        this.sensorLocations.add(sensorLocationData);
    }

    public long getRequestSensorCollectTime(int foxId, long sensorBeginTime) {
        for (CollectData data : this.collectData) {
            if (data.getFoxNodeId() == foxId) {
                List<SensorValue> values = data.getData();
                long latestSensorTime = getLatestSensorSampleTime(values);
                if (latestSensorTime < sensorBeginTime) {
                    return sensorBeginTime;
                } else {
                    return latestSensorTime + 1;
                }
            }
        }
        return sensorBeginTime;
    }

    private long getLatestSensorSampleTime(List<SensorValue> values) {
        if (values.size() <= 0) {
            return -1;
        }
        SensorValue lastValue = values.get(values.size() - 1);
        long largestRts = lastValue.getTs();
        for (int index=0; index < values.size() - 1; index++) {
            SensorValue value = values.get(index);
            if (value.getTs() > largestRts) {
                largestRts = value.getTs();
            }
        }
        return largestRts;
    }


    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public int getFoundCount() {
        return foundCount;
    }

    public void setFoundCount(int foundCount) {
        this.foundCount = foundCount;
    }

    public int getSortieCount() {
        return sortieCount;
    }

    public void setSortieCount(int sortieCount) {
        this.sortieCount = sortieCount;
    }

    public long getEarliestTakeOff() {
        return earliestTakeOff;
    }

    public void setEarliestTakeOff(long earliestTakeOff) {
        this.earliestTakeOff = earliestTakeOff;
    }

    public long getLastLanding() {
        return lastLanding;
    }

    public void setLastLanding(long lastLanding) {
        this.lastLanding = lastLanding;
    }

    public long getFirstDumpTime() {
        return firstDumpTime;
    }

    public void setFirstDumpTime(long firstDumpTime) {
        this.firstDumpTime = firstDumpTime;
    }

    public long getLastDumpTime() {
        return lastDumpTime;
    }

    public void setLastDumpTime(long lastDumpTime) {
        this.lastDumpTime = lastDumpTime;
    }

    public int getTotalSampleCount() {
        return totalSampleCount;
    }

    public void setTotalSampleCount(int totalSampleCount) {
        this.totalSampleCount = totalSampleCount;
    }

    public ArrayList<CollectData> getCollectData() {
        return collectData;
    }

    public void setCollectData(ArrayList<CollectData> collectData) {
        this.collectData = collectData;
    }

    public ArrayList<SensorLocationData> getSensorLocations() {
        return sensorLocations;
    }

    public void setSensorLocations(
        ArrayList<SensorLocationData> sensorLocations) {
        this.sensorLocations = sensorLocations;
    }
}
