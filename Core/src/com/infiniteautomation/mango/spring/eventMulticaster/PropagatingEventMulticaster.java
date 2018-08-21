/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.eventMulticaster;

import java.util.concurrent.ForkJoinPool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;

/**
 * A spring event multicaster that can propagate events to other multicasters. Typically used to propagate events from the
 * runtime context (e.g. DAO events) to the spring web context (e.g. to notify web sockets)
 *
 * @author Jared Wiltshire
 */

public class PropagatingEventMulticaster extends SimpleApplicationEventMulticaster {

    private final EventMulticasterRegistry registry;

    public PropagatingEventMulticaster(EventMulticasterRegistry registry) {
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

    @Override
    public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        // event has already been propagated, don't send it to other multicasters, just notify our listeners of the original event
        if (event instanceof PropagatedEvent) {
            PropagatedEvent propagatedEvent = (PropagatedEvent) event;
            super.multicastEvent(propagatedEvent.event, propagatedEvent.eventType);
            return;
        }

        // notify our listeners
        super.multicastEvent(event, eventType);

        // propagate the event to other multicasters
        if (event instanceof PropagatingEvent) {
            PropagatedEvent propagatedEvent = null;

            for (ApplicationEventMulticaster registeredMulticaster : this.registry.getMulticasters()) {
                // dont want to propagate to ourself
                if (registeredMulticaster == this) {
                    continue;
                }

                if (registeredMulticaster instanceof PropagatingEventMulticaster) {
                    if (propagatedEvent == null) {
                        propagatedEvent = new PropagatedEvent(this, event, eventType);
                    }
                    registeredMulticaster.multicastEvent(propagatedEvent, null);
                } else {
                    // if the child isn't a MangoEventMulticaster it wont unwrap the PropagatedEvent, notify it of original event
                    registeredMulticaster.multicastEvent(event, eventType);
                }
            }
        }
    }

    private static final class PropagatedEvent extends ApplicationEvent {
        private static final long serialVersionUID = 1L;

        private final ApplicationEvent event;
        private final ResolvableType eventType;

        public PropagatedEvent(PropagatingEventMulticaster source, ApplicationEvent event, ResolvableType eventType) {
            super(source);
            this.event = event;
            this.eventType = eventType;
        }
    }
}
