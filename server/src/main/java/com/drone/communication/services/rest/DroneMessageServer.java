package com.drone.communication.services.rest;

import com.drone.ServerUtils;
import com.drone.Utils;
import com.drone.communication.DroneMessenger;
import com.drone.communication.serviceinterface.Message;
import com.drone.data.DroneGpsReading;
import com.drone.data.DroneGpsRelayMessage;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.EOFException;
import java.io.IOException;

@Path("/messenger")
public class DroneMessageServer {
    @POST
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces(MediaType.TEXT_PLAIN)
    public String onMessageReceive(String messageJson) {
        //        String messageJson = Utils.toJson(message);
        System.out.println("Receive message " + messageJson);
        try {
            DroneGpsRelayMessage relayMessage = Utils.fromJson(messageJson, DroneGpsRelayMessage.class);
            if (relayMessage != null
                && "drone-gps".equalsIgnoreCase(relayMessage.getMessageType())
                && relayMessage.getReading() != null) {
                DroneGpsReading reading = relayMessage.getReading();
                System.out.println(String.format(
                    "[DRONE GPS RELAY] lat=%s lon=%s alt=%s speed=%s fix=%s sats=%s utc=%s local=%d",
                    reading.getLatitude(),
                    reading.getLongitude(),
                    reading.getAltitude(),
                    reading.getSpeed(),
                    reading.getFixQuality(),
                    reading.getSatelliteCount(),
                    reading.getUtcTimestamp(),
                    reading.getLocalTimestamp()
                ));
                return "OK";
            }

//            //            Message message = Utils.fromJson(messageJson, Message.class);
//            //            DroneMessenger.getInstance().receiveMessage(message);
            String ipAddress = "192.168.40.20";
////            ServerUtils.processClientData(messageJson, ipAddress);
//            String threadLabel = "Thread start time: " + System.currentTimeMillis();
//            Thread thread = Utils.create(threadLabel, () -> {
                ServerUtils.processClientData(messageJson, ipAddress);
//            });
//            thread.start();

                        return "OK";
        } catch (Exception exception) {
            exception.printStackTrace();
                        return "FAIL";
        }
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Path("/send")
   // @Produces(MediaType.TEXT_PLAIN)
    public String send(Message message) {
        try {
            String messageJson = Utils.toJson(message);
            System.out.println(
                "Sending message to other drones {}" + messageJson);
//            Message message = Utils.fromJson(messageJson, Message.class);
            DroneMessenger.getInstance().sendMessage(message);
            return "OK";
        } catch (Exception exception) {
            exception.printStackTrace();
            return "FAIL";
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String test() {
        return "OK";
    }
}
