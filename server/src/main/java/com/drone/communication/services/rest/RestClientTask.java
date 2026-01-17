package com.drone.communication.services.rest;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class RestClientTask implements Runnable {
    private final String url;
    private final String messageJson;

    public RestClientTask(String url, String messageJson) {
        this.url = url;
        this.messageJson = messageJson;
    }

    @Override
    public void run() {
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
