package com.drone.communication;

import com.drone.Utils;
import com.drone.communication.serviceinterface.Message;
import com.drone.communication.serviceinterface.Messenger;
import com.drone.communication.serviceinterface.SenderInfo;
import com.drone.communication.serviceinterface.Target;
import com.drone.communication.services.rest.RestMessengerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class DroneMessengerTest {
    @Test
    public void test() {
        SenderInfo senderInfo = new SenderInfo("1", "http://localhost:8080/messenger");
        Target target = new Target();
        target.setId("1");
        target.setTargetUrlPath("http://localhost:8081/messenger");
        ArrayList<Target> targets = new ArrayList<>();
        targets.add(target);
        Message message = new Message("1", "1", "Test Message", senderInfo, targets);
        String jsonString = Utils.toJson(message);
        System.out.println("jsonString = " + jsonString);

        Messenger messenger = DroneMessenger.getInstance();
        Assertions.assertTrue((messenger instanceof RestMessengerImpl), "message = " + jsonString);
    }
}
