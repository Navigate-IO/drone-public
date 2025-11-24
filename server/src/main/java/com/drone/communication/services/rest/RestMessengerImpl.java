package com.drone.communication.services.rest;

import com.drone.Utils;
import com.drone.communication.serviceinterface.Message;
import com.drone.communication.serviceinterface.MessageProcessor;
import com.drone.communication.serviceinterface.Messenger;
import com.drone.communication.serviceinterface.Target;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RestMessengerImpl implements Messenger {

    private List<MessageProcessor> messageProcessors;
    public RestMessengerImpl() {
        messageProcessors = new CopyOnWriteArrayList<>();
    }

    @Override
    public void sendMessage(Message message) {
        for (Target target : message.getTargets()) {
            try {
                String sensorDataJson = message.getMessage();
                sendToHttpRestServer(target.getTargetUrlPath(), sensorDataJson);
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void receiveMessage(Message message) {
        for (MessageProcessor processor : messageProcessors) {
            String jsonString = Utils.toJson(message);
            processor.processMessage(jsonString);
        }
    }

    @Override
    public void subscribe(MessageProcessor messageProcessor) {
        messageProcessors.add(messageProcessor);
    }

    public static void sendToHttpRestServer(String url,
        String messageJson)
        throws IOException, InterruptedException {
        String jsonPayload = messageJson;

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);

        // Send the POST request with the JSON payload and receive the response
        Response response = target.request(MediaType.TEXT_PLAIN)
            .post(Entity.entity(jsonPayload, MediaType.TEXT_PLAIN));

        try {
            System.out.println("Status Code: " + response.getStatus());
        } finally {
            response.close();
            client.close();
        }
    }
}
