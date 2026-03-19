package com.drone;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class LteBridgeClient {
    private static final String LTE_SEND_URL = "http://localhost:8099/lte/send";
    private static final String LTE_BROADCAST_URL = "http://localhost:8099/lte/broadcast";
    private static final int HTTP_TIMEOUT_MS = 2000;
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        2,
        2,
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(200),
        new ThreadPoolExecutor.DiscardPolicy()
    );
    private static volatile boolean shutdownHookRegistered = false;

    private LteBridgeClient() {
    }

    public static void sendToTargets(List<String> targets, String payload, String endpoint) {
        if (payload == null || payload.isBlank() || endpoint == null || endpoint.isBlank()) {
            return;
        }

        if (targets == null || targets.isEmpty()) {
            sendBroadcast(payload, endpoint);
            return;
        }

        for (String target : targets) {
            if (target == null || target.isBlank()) {
                continue;
            }
            String requestBody = "{\"target\":" + Utils.toJson(target.trim())
                + ",\"payload\":" + Utils.toJson(payload)
                + ",\"endpoint\":" + Utils.toJson(endpoint) + "}";
            fireAndForgetPost(LTE_SEND_URL, requestBody);
        }
    }

    public static void sendBroadcast(String payload, String endpoint) {
        if (payload == null || payload.isBlank() || endpoint == null || endpoint.isBlank()) {
            return;
        }

        String requestBody = "{\"payload\":" + Utils.toJson(payload)
            + ",\"endpoint\":" + Utils.toJson(endpoint) + "}";
        fireAndForgetPost(LTE_BROADCAST_URL, requestBody);
    }

    private static void fireAndForgetPost(String url, String body) {
        registerShutdownHookIfNeeded();
        try {
            executor.execute(() -> doPost(url, body));
        } catch (Exception ignored) {
        }
    }

    private static void doPost(String endpointUrl, String body) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpointUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setDoOutput(true);

            byte[] requestBytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBytes);
            }
            connection.getResponseCode();
        } catch (Exception ignored) {
            // LTE bridge is optional.
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static synchronized void registerShutdownHookIfNeeded() {
        if (shutdownHookRegistered) {
            return;
        }
        shutdownHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }
}
