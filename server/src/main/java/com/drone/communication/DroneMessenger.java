package com.drone.communication;

import com.drone.communication.serviceinterface.Messenger;
import com.drone.communication.services.rest.RestMessengerImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

public class DroneMessenger {
    private static ConcurrentHashMap<String, Messenger> messengers =
        new ConcurrentHashMap<>();

    public static Messenger getInstance() {
        return getInstance(RestMessengerImpl.class);
    }

    public static Messenger getInstance(Class<? extends Messenger> clazz) {
        if (messengers.containsKey(clazz.getName())) {
            return messengers.get(clazz.getName());
        } else {
            Messenger messenger = loadMessenger(clazz);
            messengers.put(clazz.getName(), messenger);
            return messenger;
        }
    }

    public static Messenger loadMessenger(Class<? extends Messenger> clazz) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            Messenger messenger = (Messenger) constructor.newInstance();
            System.out.println("Loaded messenger " + messenger.getClass().getName());
            return messenger;

        } catch (InstantiationException | NoSuchMethodException |
            InvocationTargetException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }
}
