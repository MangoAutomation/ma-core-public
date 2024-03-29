/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging;

import java.util.concurrent.CompletionStage;

import com.serotonin.util.ILifecycle;

/**
 * The message transport allows for a common API in Mango to send and receive messages
 *
 * @author Terry Packer
 */
public interface MessageTransport extends ILifecycle {

    /**
     * Does this transport support sending this type of message?
     */
    public boolean supportsSending(Message type);

    /**
     * Does this transport support receiving this type of message?
     */
    public boolean supportsReceiving(Message type);

    /**
     * Send a message, must be done in separate thread to not
     *  slow down Core processing
     */
    public CompletionStage<SentMessage> sendMessage(Message message);


    /**
     * Add listener for received messages
     */
    public void addListener(MessageReceivedListener listener);

    /**
     * Remove a listener
     */
    public void removeListener(MessageReceivedListener listener);
}
