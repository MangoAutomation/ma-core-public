/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.eventMulticaster;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

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

/**
 * A spring event multicaster that can propagate events to other multicasters. Typically used to propagate events from the
 * runtime context (e.g. DAO events) to the spring web context (e.g. to notify web sockets)
 *
 * @author Jared Wiltshire
 */

public class PropagatingEventMulticaster extends SimpleApplicationEventMulticaster {
    // max #workers - 1
    static final int MAX_CAP = 0x7fff;
    private final Log log = LogFactory.getLog(PropagatingEventMulticaster.class);

    private final ApplicationContext context;
    private final EventMulticasterRegistry registry;

    public PropagatingEventMulticaster(ApplicationContext context, EventMulticasterRegistry registry) {
        super();
        //We need our threads to have access to the class loader that contains our module loaded classes,
        //  so we set the class loader for any threads that are created.  The parameters for this constructor
        //  were lifted from ForkJoinPool.makeCommonPool().  Note that we are not catering for a Security Manager
        //  by using this code as the ForkJoinPool.makeCommonPool() does.
        int parallelism = -1;
        if (parallelism < 0 && // default 1 less than #cores
            (parallelism = Runtime.getRuntime().availableProcessors() - 1) <= 0)
            parallelism = 1;
        if (parallelism > MAX_CAP)
            parallelism = MAX_CAP;
        ForkJoinPool pool = new ForkJoinPool(parallelism,
                new ForkJoinWorkerThreadFactory() {
                    @Override
                    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                        return new MangoForkJoinWorkerThread(context.getClassLoader(), pool);
                    }
                }, null, false);
        this.setTaskExecutor(pool);
        this.registry = registry;
        this.context = context;
    }

    class MangoForkJoinWorkerThread extends ForkJoinWorkerThread {

        /**
         * @param classLoader 
         * @param pool
         */
        protected MangoForkJoinWorkerThread(ClassLoader classLoader, ForkJoinPool pool) {
            super(pool);
            this.setContextClassLoader(classLoader);
        }
        
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

        for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
            executor.execute(() -> {
                if (log.isDebugEnabled()) {
                    log.debug("Invoking listeners for " + event);
                }
                if (((ConfigurableApplicationContext) context).isActive()) {
                    invokeListener(listener, event);
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
}
