package com.drone.communication.services.rest;

import com.drone.Utils;
import com.drone.communication.serviceinterface.Message;
import com.drone.communication.serviceinterface.MessageProcessor;
import com.drone.communication.serviceinterface.Messenger;
import com.drone.communication.serviceinterface.Target;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RestMessengerImpl implements Messenger {
    private static final int THREAD_POOL_SIZE = 5;
    private static final int MAX_PENDING_SEND_TASKS = 200;
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        THREAD_POOL_SIZE,
        THREAD_POOL_SIZE,
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(MAX_PENDING_SEND_TASKS),
        new ThreadPoolExecutor.AbortPolicy()
    );
    private static boolean justOnce = false;

    private List<MessageProcessor> messageProcessors;
    public RestMessengerImpl() {
        messageProcessors = new CopyOnWriteArrayList<>();

    }

    @Override
    public void sendMessage(Message message) {
        for (Target target : message.getTargets()) {
            String sensorDataJson = message.getMessage();
            sendToHttpRestServer(target.getTargetUrlPath(), sensorDataJson);
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

    public static void sendToHttpRestServer(String url, String messageJson) {
        try {
            executor.execute(new RestClientTask(url, messageJson));
        } catch (Exception ignored) {
        }
        if (!justOnce) {
            justOnce = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                executor.shutdown();
            }));
        }
    }
}
