package com.drone;

import com.drone.communication.DroneMessenger;
import com.drone.communication.serviceinterface.Message;
import com.drone.communication.serviceinterface.Messenger;
import com.drone.communication.serviceinterface.SenderInfo;
import com.drone.communication.serviceinterface.Target;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

public class MessengerUtils {
    private static int sequence = 0;
    public static synchronized void sendToDrones(String sensorDataJson) {
        List<String> otherDronesURLs = getOtherDronesUrlsList();
        if (otherDronesURLs.isEmpty()) {
            return;
        }
        List<Target> targets = new ArrayList<>();
        for (String droneUrl : otherDronesURLs) {
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

    public static List<String> getOtherDronesUrlsList() {
        String otherDronesURLs = Config.getInstance().getOtherDronesUrls();
        if (otherDronesURLs == null || otherDronesURLs.isBlank()) {
            return new ArrayList<>();
        }

        StringTokenizer stringTokenizer = new StringTokenizer(otherDronesURLs, ",");
        List<String> urls = new ArrayList<>();
        while (stringTokenizer.hasMoreTokens()) {
            String droneUrl = stringTokenizer.nextToken().trim();
            if (!droneUrl.isEmpty()) {
                urls.add(droneUrl);
            }
        }
        return urls;
    }

    public static List<String> resolveLteTargetsForMessage(Message message) {
        Set<String> resolvedTargets = new LinkedHashSet<>();
        List<String> configuredLteTargets = Config.getInstance().getLteDroneTargets();

        if (message != null && message.getTargets() != null) {
            List<String> configuredUrls = getOtherDronesUrlsList();
            for (Target target : message.getTargets()) {
                if (target == null) {
                    continue;
                }

                String targetId = target.getId();
                if (targetId != null && !targetId.isBlank()) {
                    resolvedTargets.add(targetId.trim());
                    continue;
                }

                String targetUrl = target.getTargetUrlPath();
                if (targetUrl == null || targetUrl.isBlank()) {
                    continue;
                }

                int idx = configuredUrls.indexOf(targetUrl.trim());
                if (idx >= 0 && idx < configuredLteTargets.size()) {
                    String configuredTarget = configuredLteTargets.get(idx);
                    if (configuredTarget != null && !configuredTarget.isBlank()) {
                        resolvedTargets.add(configuredTarget.trim());
                    }
                }
            }
        }

        if (resolvedTargets.isEmpty()) {
            for (String configuredTarget : configuredLteTargets) {
                if (configuredTarget != null && !configuredTarget.isBlank()) {
                    resolvedTargets.add(configuredTarget.trim());
                }
            }
        }

        return new ArrayList<>(resolvedTargets);
    }

    private static String getSequenceNumber() {
        String ipAddress = Config.getInstance().getActualIpAddress();
        String sequenceNumber = ipAddress + "_" + sequence;
        return sequenceNumber;
    }
}
