/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.io.messaging.Message;
import com.infiniteautomation.mango.io.messaging.MessageManager;
import com.infiniteautomation.mango.io.messaging.MessageReceivedListener;
import com.infiniteautomation.mango.io.messaging.MessageTransport;
import com.infiniteautomation.mango.io.messaging.NoTransportAvailableException;
import com.infiniteautomation.mango.io.messaging.SentMessage;
import com.serotonin.util.LifecycleException;

/**
 *
 * @author Terry Packer
 */
@Service
public class MessageManagerImpl implements MessageManager,MessageReceivedListener {

    private final List<MessageReceivedListener> listeners = new CopyOnWriteArrayList<MessageReceivedListener>();
    private final List<MessageTransport> senders;

    @Autowired
    public MessageManagerImpl(List<MessageTransport> senders) {
        this.senders = senders;
        for(MessageTransport sender : senders) {
            sender.addListener(this);
        }
    }

    @Override
    public void initialize(boolean safe) throws LifecycleException {
        for(MessageTransport sender : this.senders) {
            sender.initialize(safe);
        }
    }

    @Override
    public void terminate() throws LifecycleException {
        for(MessageTransport sender : this.senders) {
            sender.terminate();
        }
    }

    @Override
    public void joinTermination() {
        for(MessageTransport sender : this.senders) {
            sender.joinTermination();
        }
    }

    @Override
    public List<CompletionStage<SentMessage>> sendMessage(Message message) {
        List<CompletionStage<SentMessage>> sent = new ArrayList<>();
        for(MessageTransport sender : this.senders) {
            if(sender.supportsSending(message)) {
                sent.add(sender.sendMessage(message));
            }
        }
        return sent;
    }

    @Override
    public CompletionStage<SentMessage> sendMessageUsingFirstAvailableTransport(Message message) {
        for(MessageTransport sender : this.senders) {
            if(sender.supportsSending(message)) {
                return sender.sendMessage(message);
            }
        }
        CompletableFuture<SentMessage> f = new CompletableFuture<>();
        f.completeExceptionally(new NoTransportAvailableException(message));
        return f;
    }


    @Override
    public List<MessageTransport> getSenders() {
        return this.senders;
    }

    @Override
    public boolean supportsReceiving(Message type) {
        return true;
    }

    @Override
    public void messageReceived(String from, Message message) {
        for(MessageReceivedListener listener : this.listeners) {
            if(listener.supportsReceiving(message)) {
                listener.messageReceived(from, message);
            }
        }
    }

    @Override
    public void addListener(MessageReceivedListener l) {
        this.listeners.add(l);
    }

    @Override
    public void removeListener(MessageReceivedListener l) {
        this.listeners.remove(l);
    }

    @Override
    public MessageTransport getPriorityTransport(Message type) throws NoTransportAvailableException {
        for(MessageTransport sender : this.senders) {
            if(sender.supportsSending(type)) {
                return sender;
            }
        }
        throw new NoTransportAvailableException(type);
    }

}
