/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging;

import java.util.List;
import java.util.concurrent.CompletionStage;

import com.serotonin.util.ILifecycle;

/**
 * Interface for the message manager api.  Since Mango may have multiple message sender modules installed the
 *  manager allows access to all based on thier priority
 *
 * @author Terry Packer
 */
public interface MessageManager extends ILifecycle  {

    /**
     * Send a message using all message senders available for the
     * specific message type
     *
     */
    public List<CompletionStage<SentMessage>> sendMessage(Message message);

    /**
     * Send the message using the first transport that will accept it
     */
    public CompletionStage<SentMessage> sendMessageUsingFirstAvailableTransport(Message message);

    /**
     * Get the highest priority transport for this type of messages
     *
     */
    MessageTransport getPriorityTransport(Message type) throws NoTransportAvailableException;

    /**
     * Get all senders running in Mango
     */
    List<MessageTransport> getSenders();


    /**
     * Add listener for received messages
     */
    public void addListener(MessageReceivedListener l);

    /**
     * Remove listener for received messages
     */
    public void removeListener(MessageReceivedListener l);


}
