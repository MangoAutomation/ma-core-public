/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging;

/**
 * Listen for recieved messages
 * @author Terry Packer
 */
public interface MessageReceivedListener {

    /**
     * Does this listener support receiving this type?
     * @param type
     * @return
     */
    public boolean supportsReceiving(MessageType type);

    /**
     * A message was recieved
     * @param from
     * @param message
     */
    public void messageReceived(String from, Message message);

}
