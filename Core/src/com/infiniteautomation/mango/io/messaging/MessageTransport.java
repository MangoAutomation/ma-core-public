/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
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
     * @param type
     * @return
     */
    public boolean supportsSending(MessageType type);

    /**
     * Does this transport support receiving this type of message?
     * @param type
     * @return
     */
    public boolean supportsReceiving(MessageType type);

    /**
     * Send a message, must be done in separate thread to not
     *  slow down Core processing
     * @param message
     * @return
     */
    public CompletionStage<SentMessage> sendMessage(Message message);


    /**
     * Add listener for received messages
     * @param listener
     */
    public void addListener(MessageReceivedListener listener);

    /**
     * Remove a listener
     * @param listener
     */
    public void removeListener(MessageReceivedListener listener);
}
