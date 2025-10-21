package com.drone.communication.services.rest;

import com.drone.Utils;
import com.drone.communication.serviceinterface.Message;
import com.drone.communication.serviceinterface.MessageProcessor;
import com.drone.communication.serviceinterface.Messenger;
import com.drone.communication.serviceinterface.Target;

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
                String contentType = "application/json";
                sendToHttpRestServer(target.getTargetUrlPath(), message,
                    contentType);
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

    public static HttpResponse<String> sendToHttpRestServer(String url,
        Message message, String contentType)
        throws IOException, InterruptedException {
        String jsonString = Utils.toJson(message);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(jsonString)).build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());
        System.out.println("Sending message" + jsonString + " to destination " + url + " with contentType=" + contentType);
        System.out.println("response code = " +  response.statusCode());

        return response;
    }
}
