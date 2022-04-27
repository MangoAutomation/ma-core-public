/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.esb;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.stereotype.Component;

/**
 * Handle for events topic
 */
@Component
public class EventsTopic extends NewTopic {
    public EventsTopic() {
        super("mango-events", 1, (short) 1);
    }
}
