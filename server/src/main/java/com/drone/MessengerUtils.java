package com.drone;

import com.drone.communication.DroneMessenger;
import com.drone.communication.serviceinterface.Message;
import com.drone.communication.serviceinterface.Messenger;
import com.drone.communication.serviceinterface.SenderInfo;
import com.drone.communication.serviceinterface.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

public class MessengerUtils {
    private static int sequence = 0;
    public static synchronized void sendToDrones(String sensorDataJson) {
        String otherDronesURLs = Config.getInstance().getOtherDronesUrls();
        if (otherDronesURLs == null || otherDronesURLs.isBlank()) {
            return;
        }

        StringTokenizer stringTokenizer = new StringTokenizer(otherDronesURLs, ",");
        List<Target> targets = new ArrayList<>();
        while (stringTokenizer.hasMoreTokens()) {
            String droneUrl = stringTokenizer.nextToken().trim();
            Target target = new Target("", droneUrl);
            targets.add(target);
        }


        String sequenceNumber = getSequenceNumber();
        String messageId = UUID.randomUUID().toString();
        SenderInfo senderInfo = new SenderInfo("", Config.getInstance()
            .getActualIpAddress());
        Message message = new Message(sequenceNumber, messageId, sensorDataJson,senderInfo, targets);
        DroneMessenger.getInstance().sendMessage(message);
    }

    private static String getSequenceNumber() {
        String ipAddress = Config.getInstance().getActualIpAddress();
        String sequenceNumber = ipAddress + "_" + sequence;
        return sequenceNumber;
    }
}
