/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging;

import java.util.Set;
import java.util.concurrent.Future;

import com.serotonin.util.ILifecycle;

/**
 * The message manager allows for a common API in Mango to send SMS and MMS style messages
 *  an SMS/MMS provider must be registered.
 *
 * @author Terry Packer
 */
public interface MessageManager extends ILifecycle {

    /**
     *
     * @param recipients
     * @param message
     * @return
     * @throws MessageSendException
     */
    public Future<SentMessage> sendMessage(Set<String> recipients, String message) throws MessageSendException;

}
