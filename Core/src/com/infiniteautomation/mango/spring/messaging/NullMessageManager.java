/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.messaging;

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.util.DoneFuture;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.io.messaging.MessageManager;
import com.infiniteautomation.mango.io.messaging.SentMessage;
import com.serotonin.util.LifecycleException;

/**
 * The default message manager implementation that does nothing but log attempted operations
 *  to indicate that no messaging module is installed.  This service has an order of 100 which means
 *  any other MessageManager with a lower order will be prioritized for use
 *
 * @author Terry Packer
 */
@Service()
@Order(100)
public class NullMessageManager implements MessageManager {

    private static final Log LOG = LogFactory.getLog(NullMessageManager.class);

    @Override
    public void initialize(boolean safe) throws LifecycleException {

    }

    @Override
    public void terminate() throws LifecycleException {

    }

    @Override
    public void joinTermination() {

    }

    @Override
    public Future<SentMessage> sendMessage(Set<String> recipients, String message) {
        LOG.info("Not sending message as no messaging module installed.");
        return new DoneFuture<SentMessage>(new NullSentMessage());
    }

    /**
     *
     * @author Terry Packer
     */
    public class NullSentMessage implements SentMessage {

    }

}
