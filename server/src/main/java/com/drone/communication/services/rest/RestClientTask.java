package com.drone.communication.services.rest;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class RestClientTask implements Runnable {
    private static final long RETRY_DELAY_MS = 5000L;
    private final String url;
    private final String messageJson;

    public RestClientTask(String url, String messageJson) {
        this.url = url;
        this.messageJson = messageJson;
    }

    @Override
    public void run() {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(url);
            while (true) {
                Response response = null;
                try {
                    response = target.request(MediaType.TEXT_PLAIN)
                        .post(Entity.entity(messageJson, MediaType.TEXT_PLAIN));

                    int status = response.getStatus();
                    if (status >= 200 && status < 300) {
                        return;
                    }

                    if (status >= 400 && status < 500 && status != 429) {
                        return;
                    }
                } catch (Exception ignored) {
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            client.close();
        }
    }
}
