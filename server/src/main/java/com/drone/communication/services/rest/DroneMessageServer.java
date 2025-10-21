package com.drone.communication.services.rest;

import com.drone.Utils;
import com.drone.communication.DroneMessenger;
import com.drone.communication.serviceinterface.Message;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/messenger")
public class DroneMessageServer {
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces(MediaType.TEXT_PLAIN)
    public String onMessageReceive(Message message) {
        String messageJson = Utils.toJson(message);

        System.out.println("Receive message " +  messageJson);
        try {
//            Message message = Utils.fromJson(messageJson, Message.class);
            DroneMessenger.getInstance().receiveMessage(message);
            return "OK";
        } catch (Exception exception) {
            exception.printStackTrace();
            return "FAIL";
        }
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Path("/send")
    @Produces(MediaType.TEXT_PLAIN)
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
