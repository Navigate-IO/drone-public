package com.drone.communication.serviceinterface;

public interface Messenger {
    public void sendMessage(Message message);
    public void receiveMessage(Message message);
    public void subscribe(MessageProcessor messageProcessor);
}
