package com.drone;

import com.drone.data.BaseAreaSensorDump;
import com.drone.data.ClientCoreData;
import com.drone.data.ClientData;
import com.drone.data.ResponseDataObject;
import com.drone.data.ServerData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class ServerUtils {
    public static ServerData processClientData(String json, String ipAddress) {
        System.out.println("Receiving SENSOR data: " + json);
        ClientData clientData =  Utils.fromJson(json,
            ClientData.class);
        writeToFile(clientData);
        String ccmd = clientData.getCore().getCcmd();
        if (ccmd.trim().equalsIgnoreCase("hi")) {
            ServerData data = new ServerData();
            data.setServ(createHiResponse(clientData.getCore(), ipAddress));
            return data;
        } else if (ccmd.trim().equalsIgnoreCase("rval")) {
            BaseAreaSensorDump.addClientData(clientData);
            ServerData data = new ServerData();
            data.setServ(createRvalResponse(clientData, ipAddress));
            return data;
        } else if (ccmd.trim().equalsIgnoreCase("rlog”")) {
            ServerData data = new ServerData();
            data.setServ(createRlogResponse(clientData.getCore(), ipAddress));
            return data;
        } else {// bye
            System.out.println("Received bye from sensor id" + clientData.getCore().getFox());
            ResponseDataObject responseDataObject = new ResponseDataObject();
            responseDataObject.setSrep("done");
            ServerData data = new ServerData();
            data.setServ(responseDataObject);
            String responseJson = Utils.toJson(data);
            System.out.println("Server Response: " +responseJson + "\n");
            return data;
        }
    }

    private static ResponseDataObject createHiResponse(ClientCoreData clientCoreData, String ipAddress) {
        ResponseDataObject result = new ResponseDataObject();
        long clientSensorOldestTimeStamp = clientCoreData.getFtso();
        long clientSensorNewestTimeStamp = clientCoreData.getFtsr();
        long clientTimeStamp = clientCoreData.getFts();
        int sampleCount = clientCoreData.getFcnt();
        String srep = "sval";
        String sip = ipAddress;
        long stsb = BaseAreaSensorDump.getInstance().getRequestSensorCollectTime(clientCoreData.getFox(), clientSensorOldestTimeStamp);
        long stse = clientSensorNewestTimeStamp;

        ResponseDataObject responseDataObject = new ResponseDataObject();
        responseDataObject.setSrep(srep);
        responseDataObject.setSip(sip);
        responseDataObject.setStsb(stsb);
        responseDataObject.setStse(stse);
        responseDataObject.setSts(clientTimeStamp);
        ServerData data = new ServerData();
        data.setServ(responseDataObject);
//        this.serverResponses.put(clientTimeStamp, data);
        Date date = new Date(clientTimeStamp * 1000);
        String responseJson = Utils.toJson(data);
        System.out.println("ccmd = hi, Time: " + date.toString());
        System.out.println("Server Response: " +responseJson );
        writeToFile(responseDataObject);
        return responseDataObject;
    }

    private static ResponseDataObject createRvalResponse(ClientData clientData, String ipAddress) {
        ResponseDataObject responseDataObject = new ResponseDataObject();
        long clientSensorOldestTimeStamp = clientData.getCore().getFtso();
        long clientSensorNewestTimeStamp = clientData.getCore().getFtsr();
        long clientTimeStamp = clientData.getCore().getFts();
        int sampleCount = clientData.getData().getDlen();
        int actualCount = clientData.getData().getDump().size();

        if (sampleCount != actualCount) {
            String srep = "ping";
            String sip = ipAddress;
            long stsb = BaseAreaSensorDump.getInstance().getRequestSensorCollectTime(clientData.getCore().getFox(), clientSensorOldestTimeStamp);
            long stse = clientSensorNewestTimeStamp;
            responseDataObject.setSrep(srep);
            responseDataObject.setSip(sip);
            responseDataObject.setStsb(stsb);
            responseDataObject.setStse(stse);
            responseDataObject.setSts(clientTimeStamp);
            ServerData data = new ServerData();
            data.setServ(responseDataObject);
//            this.serverResponses.put(clientTimeStamp, data);
            String responseJson = Utils.toJson(data);
            Date date = new Date(clientTimeStamp * 1000);
            System.out.println("ccmd = rval, Time: " + date.toString());
            System.out.println("Server Response: " +responseJson );
            writeToFile(responseDataObject);
            return responseDataObject;

        } else {
            String srep = "buby";
            String sip = ipAddress;
            long stsb = BaseAreaSensorDump.getInstance().getRequestSensorCollectTime(clientData.getCore().getFox(), clientSensorOldestTimeStamp);
            long stse = clientSensorNewestTimeStamp;
            responseDataObject.setSrep(srep);
            responseDataObject.setSip(sip);
            responseDataObject.setStsb(stsb);
            responseDataObject.setStse(stse);
            responseDataObject.setSts(clientTimeStamp);
            ServerData data = new ServerData();
            data.setServ(responseDataObject);
//            this.serverResponses.put(clientTimeStamp, data);
            Date date = new Date(clientTimeStamp * 1000);
            String responseJson = Utils.toJson(data);
            System.out.println("ccmd = rval, Time: " + date.toString());
            System.out.println("Server Response: " +responseJson );
            writeToFile(responseDataObject);
            return responseDataObject;
        }
    }

    public static ResponseDataObject createRlogResponse(ClientCoreData sensorData, String ipAddress) {
        ResponseDataObject responseDataObject = new ResponseDataObject();
        //        long timeStamp = getTimeStamp();
        long clientSensorOldestTimeStamp = sensorData.getFtso();
        long clientSensorNewestTimeStamp = sensorData.getFtsr();
        long clientTimeStamp = sensorData.getFts();

        String srep = "buby";
        String sip = ipAddress;
        long stsb = BaseAreaSensorDump.getInstance().getRequestSensorCollectTime(sensorData.getFox(), clientSensorOldestTimeStamp);
        long stse = clientSensorNewestTimeStamp;
        responseDataObject.setSrep(srep);
        responseDataObject.setSip(sip);
        responseDataObject.setStsb(stsb);
        responseDataObject.setStse(stse);
        responseDataObject.setSts(clientTimeStamp);
        ServerData data = new ServerData();
        data.setServ(responseDataObject);
//        this.serverResponses.put(clientTimeStamp, data);
        Date date = new Date(clientTimeStamp * 1000);
        String responseJson = Utils.toJson(responseDataObject);
        System.out.println("ccmd = rlog, Time: " + date.toString());
        System.out.println("Server Response: " +responseJson );
        writeToFile(responseDataObject);
        return responseDataObject;
    }

    public static void writeToFiles(String json) {
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
            fileWriter.write(json);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeToFile(Object object) {
        String folder = "Output";
        File folderFile = new File(folder);
        if (!(folderFile.exists())) {
            folderFile.mkdirs();
        }
        String folderName = "messages";
        File subFolder = new File(folderFile, folderName);
        if (!(subFolder.exists())) {
            subFolder.mkdirs();
        }

        long timeStamp = System.currentTimeMillis();
        String fileName = object.getClass().getName() + "_" + timeStamp + ".json";
        File file4 = new File(subFolder, fileName);
        try (FileWriter fileWriter = new FileWriter(file4)) {
            String json4 = Utils.toJson(object);
            fileWriter.write(json4);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
