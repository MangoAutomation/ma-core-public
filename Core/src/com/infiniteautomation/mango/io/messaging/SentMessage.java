/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging;

/**
 *
 * @author Terry Packer
 */
public interface SentMessage {

    /**
     * The transport that sent me
     * @return
     */
    MessageTransport getTransport();

}
