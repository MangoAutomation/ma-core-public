/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.eventMulticaster;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;
import org.springframework.util.ErrorHandler;

import com.serotonin.m2m2.TerminationReason;

/**
 * A spring event multicaster that can propagate events to other multicasters. Typically used to propagate events from the
 * runtime context (e.g. DAO events) to the spring web context (e.g. to notify web sockets)
 *
 * @author Jared Wiltshire
 */

public class PropagatingEventMulticaster extends SimpleApplicationEventMulticaster {

    private final static Log log = LogFactory.getLog(PropagatingEventMulticaster.class);

    private final ApplicationContext context;
    private final EventMulticasterRegistry registry;

    public PropagatingEventMulticaster(ApplicationContext context, EventMulticasterRegistry registry, Executor executor) {
        super();
        this.setTaskExecutor(executor);
        this.registry = registry;
        this.context = context;
    }

    @PostConstruct
    protected void init() {
        this.registry.register(this);
        this.setErrorHandler(new EventMulticasterErrorHandler());
    }

    @PreDestroy
    protected void destroy() {
        this.registry.unregister(this);
    }

    @Override
    public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        // Don't propagate ApplicationContextEvents to other contexts
        // Also don't use async executor due to a bug in FrameworkServlet
        // https://jira.spring.io/projects/SPR/issues/SPR-17442
        if (event instanceof ApplicationContextEvent) {
            this.doMulticastEvent(event, eventType);
            return;
        }

        // the multicast only happens from the root context, i.e. when the context has no parent
        if (this.context.getParent() == null) {
            for (ApplicationEventMulticaster registeredMulticaster : this.registry.getMulticasters()) {
                if (registeredMulticaster instanceof PropagatingEventMulticaster) {
                    ((PropagatingEventMulticaster) registeredMulticaster).doMulticastEventWithExecutor(event, eventType);
                } else {
                    registeredMulticaster.multicastEvent(event, eventType);
                }
            }
        }
    }

    private void doMulticastEventWithExecutor(ApplicationEvent event, ResolvableType eventType) {
        ResolvableType type = (eventType != null ? eventType : ResolvableType.forInstance(event));

        Executor executor = this.getTaskExecutor();

        if (log.isDebugEnabled()) {
            log.debug("Invoking listeners for " + event);
        }

        for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
            executor.execute(() -> {
                try {
                    if (((ConfigurableApplicationContext) context).isActive()) {
                        invokeListener(listener, event);
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Error invoking listener " + listener + " for " + event, e);
                    }
                }
            });
        }
    }

    private void doMulticastEvent(ApplicationEvent event, ResolvableType eventType) {
        ResolvableType type = (eventType != null ? eventType : ResolvableType.forInstance(event));

        for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
            invokeListener(listener, event);
        }
    }

    /**
     * Handle all event multicasting errors by logging them.  Exit Mango on OOM errors
     */
    private static class EventMulticasterErrorHandler implements ErrorHandler {

        @Override
        public void handleError(Throwable t) {
            if(t instanceof UndeclaredThrowableException) {
                Throwable source = ((UndeclaredThrowableException)t).getUndeclaredThrowable();
                if (source instanceof OutOfMemoryError) {
                    log.fatal("Out Of Memory exception in thread " + Thread.currentThread().getName() + " Mango will now terminate.", source);
                    System.exit(TerminationReason.OUT_OF_MEMORY_ERROR.getExitStatus());
                }else {
                    log.error("Error multicasting event", source);
                }
            } else {
                log.error("Error multicasting event", t);
            }
        }
    }
}
