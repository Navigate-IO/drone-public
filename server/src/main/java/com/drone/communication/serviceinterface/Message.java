package com.drone.communication.serviceinterface;

import java.util.List;

public class Message {
    private String sequenceId;
    private String messageId;
    private String message;
    private SenderInfo senderInfo;
    private List<Target> targets;

    public Message() {}
    public Message(String sequenceId, String messageId, String message, SenderInfo senderInfo, List<Target> targets) {
        this.senderInfo =senderInfo;
        this.sequenceId = sequenceId;
        this.message = message;
        this.messageId = messageId;
        this.targets = targets;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SenderInfo getSenderInfo() {
        return senderInfo;
    }

    public void setSenderInfo(SenderInfo senderInfo) {
        this.senderInfo = senderInfo;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }
}
