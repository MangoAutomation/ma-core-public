/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.eventMulticaster;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.context.event.ApplicationEventMulticaster;

/**
 * Used by {@link PropagatingEventMulticaster} to notify child event multicasters of events from the root context
 *
 * @author Jared Wiltshire
 */
public class EventMulticasterRegistry {

    private final Set<ApplicationEventMulticaster> multicasters = new CopyOnWriteArraySet<ApplicationEventMulticaster>();

    public void register(ApplicationEventMulticaster child) {
        this.multicasters.add(child);
    }

    public void unregister(ApplicationEventMulticaster child) {
        this.multicasters.remove(child);
    }

    Set<ApplicationEventMulticaster> getMulticasters() {
        return multicasters;
    }
}
