package com.drone;

import com.drone.data.BaseAreaSensorDump;
import com.drone.data.ClientCoreData;
import com.drone.data.ClientData;
import com.drone.data.ClientLogData;
//import com.drone.data.ClientResponseWithSensorData;
import com.drone.data.CollectData;
import com.drone.data.ResponseDataObject;
//import com.drone.data.ClientResponseWithLogData;
import com.drone.data.SensorData;
import com.drone.data.SensorValue;
import com.drone.data.ServerData;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//Sets the path to base URL + /hello
@Path("/")
public class DataHandler {
    public static String ipAddress = "192.168.40.20";
    public static long takeOffTime = System.currentTimeMillis();
    private ConcurrentHashMap<Long, ServerData> serverResponses = new ConcurrentHashMap<>();
    int count = 0;

    public void writeToFiles(String json) {
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

    public void writeToFile(Object object) {
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
//            throw new RuntimeException(e);
            e.printStackTrace();
        }
    }

    // This method is called if TEXT_PLAIN is requested
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayPlainTextHello() {
        String json;
        if (count%2 == 0) {
            ClientCoreData data = new ClientCoreData();
            json = Utils.toJson(data);
        } else {
            SensorData sensorData = new SensorData();

            json = Utils.toJson(sensorData);
            System.out.println(json);
        }
        count++;
        return json;
    }

    // This method is called if XML is requested
    @GET
    @Produces(MediaType.TEXT_XML)
    public String sayXMLHello() {

        String json;
        if (count%2 == 0) {
            ClientCoreData data = new ClientCoreData();
            json = Utils.toJson(data);
        } else {
            SensorData sensorData = new SensorData();
            json = Utils.toJson(sensorData);
            System.out.println(json);
        }
        count++;
        return "<?xml version=\"1.0\"?>" + "<hello>" + json + "</hello>";
    }

    // This method is called if HTML is requested
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String initialize() {
        takeOffTime = System.currentTimeMillis();
        String json;

            SensorData sensorData = new SensorData();
            json = Utils.toJson(sensorData);
            System.out.println(json);
        count++;
        return "<html> " + "<title> hello </title>"
            + "<body><h1>" + json + "</body></h1>" + "</html> ";
    }

    // This method is called if HTML is requested
    @Path("/update")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String sayHtmlUpdateHello() {
        String json;
        SensorData sensorData = new SensorData();
        json = Utils.toJson(sensorData);
        System.out.println(json);
        //        }
        count++;
        return "<html> " + "<title> hello </title>"
            + "<body><h1>" + json + "</body></h1>" + "</html> ";
    }

    // This method is called if HTML is requested
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BaseAreaSensorDump getServerData() {
        BaseAreaSensorDump baseAreaSensorDump = BaseAreaSensorDump.getInstance();
        String json = BaseAreaSensorDump.collectData();
        writeToFiles(json);
        return baseAreaSensorDump;
    }

    @GET
    @Path("/getBigDump")
    @Produces(MediaType.APPLICATION_JSON)
    public BaseAreaSensorDump getBigDumpData() {
        BaseAreaSensorDump baseAreaSensorDump = BaseAreaSensorDump.getInstance();
        String json = BaseAreaSensorDump.collectData();
        writeToFiles(json);
        return baseAreaSensorDump;
    }

    @GET
    @Path("/getIncrementalDump")
    @Produces(MediaType.APPLICATION_JSON)
    public String getIncrementalData() {
        String result = BaseAreaSensorDump.collectIncrementData();
        BaseAreaSensorDump.resetIncrementalData();
        return result;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ServerData getClientData(String json) {
        System.out.println("Received data from sensor =" + json);
        try {
            return processClientData(json);
        } catch (Exception exception) {
            exception.printStackTrace();
            ResponseDataObject responseDataObject = new ResponseDataObject();
            ServerData data = new ServerData();
            data.setServ(responseDataObject);
            return data;
        }
    }

    private ServerData processClientData(String json) {
        ClientData clientData =  Utils.fromJson(json,
            ClientData.class);
        writeToFile(clientData);
        String ccmd = clientData.getCore().getCcmd();
        if (ccmd.trim().equalsIgnoreCase("hi")) {
            ServerData data = new ServerData();
            data.setServ(createHiResponse(clientData.getCore()));
            return data;
        } else if (ccmd.trim().equalsIgnoreCase("rval")) {
            BaseAreaSensorDump.addClientData(clientData);
//            BaseAreaSensorDump.getInstance().addSensorData(clientData);
            ServerData data = new ServerData();
            data.setServ(createRvalResponse(clientData));
            return data;
        } else if (ccmd.trim().equalsIgnoreCase("rlog”")) {
//            ClientResponseWithLogData logData = Utils.fromJson(json, ClientResponseWithLogData.class);
            ServerData data = new ServerData();
            data.setServ(createRlogResponse(clientData.getCore()));
            return data;
//            return createRlogResponse(logData);
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

    private long getTimeStamp() {
        long result = System.currentTimeMillis() / 1000;
        return result;
    }

    private ResponseDataObject createHiResponse(ClientCoreData clientCoreData) {
        ResponseDataObject result = new ResponseDataObject();
        long clientSensorOldestTimeStamp = clientCoreData.getFtso();
        long clientSensorNewestTimeStamp = clientCoreData.getFtsr();
        long clientTimeStamp = clientCoreData.getFts();
        int sampleCount = clientCoreData.getFcnt();
        String srep = "sval";
        String sip = ipAddress;
//        long stsb = clientSensorOldestTimeStamp;
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
        this.serverResponses.put(clientTimeStamp, data);
        Date date = new Date(clientTimeStamp * 1000);
        String responseJson = Utils.toJson(data);
        System.out.println("ccmd = hi, Time: " + date.toString());
        System.out.println("Server Response: " +responseJson );
        writeToFile(responseDataObject);
        return responseDataObject;
    }

    private ResponseDataObject createRvalResponse(ClientData clientData) {
        ResponseDataObject responseDataObject = new ResponseDataObject();
//        long timeStamp = getTimeStamp();
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
            this.serverResponses.put(clientTimeStamp, data);
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
            this.serverResponses.put(clientTimeStamp, data);
            Date date = new Date(clientTimeStamp * 1000);
            String responseJson = Utils.toJson(data);
            System.out.println("ccmd = rval, Time: " + date.toString());
            System.out.println("Server Response: " +responseJson );
            writeToFile(responseDataObject);
            return responseDataObject;
        }
    }

    public ResponseDataObject createRlogResponse(ClientCoreData sensorData) {
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
        this.serverResponses.put(clientTimeStamp, data);
        Date date = new Date(clientTimeStamp * 1000);
        String responseJson = Utils.toJson(responseDataObject);
        System.out.println("ccmd = rlog, Time: " + date.toString());
        System.out.println("Server Response: " +responseJson );
        writeToFile(responseDataObject);
        return responseDataObject;
    }


    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ServerData getClientUpdateData(String json) {
        System.out.println("Received data from sensor =" + json);
        try {
            return processClientData(json);
        } catch (Exception exception) {
            exception.printStackTrace();
            ResponseDataObject responseDataObject = new ResponseDataObject();
            ServerData data = new ServerData();
            data.setServ(responseDataObject);
            return data;
        }
    }
}