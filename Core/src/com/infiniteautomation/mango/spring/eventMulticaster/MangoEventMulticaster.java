/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.eventMulticaster;

import java.util.concurrent.ForkJoinPool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.event.SimpleApplicationEventMulticaster;

/**
 * A spring event multicaster that registers for propagated events and uses a ForkJoinPool executor.
 *
 * @author Jared Wiltshire
 */

public class MangoEventMulticaster extends SimpleApplicationEventMulticaster {

    private final EventMulticasterRegistry registry;

    public MangoEventMulticaster(EventMulticasterRegistry registry) {
        super();
        this.setTaskExecutor(ForkJoinPool.commonPool());
        this.registry = registry;
    }

    @PostConstruct
    protected void init() {
        this.registry.register(this);
    }

    @PreDestroy
    protected void destroy() {
        this.registry.unregister(this);
    }

}
