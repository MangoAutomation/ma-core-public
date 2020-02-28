/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging;

/**
 *
 * @author Terry Packer
 */
public interface Message {

    /**
     * Type of message
     * @return
     */
    public MessageType getType();

}
