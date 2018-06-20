/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;

/**
 * @author Jared Wiltshire
 */
public class ApplicationListenerAdapter<T extends ApplicationEvent> implements GenericApplicationListener {

    private final ApplicationListener<T> delegate;
    private final Class<T> supportedEventType;

    public ApplicationListenerAdapter(ApplicationListener<T> delegate, Class<T> supportedEventType) {
        this.delegate = delegate;
        this.supportedEventType = supportedEventType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        this.delegate.onApplicationEvent((T) event);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return supportedEventType.isAssignableFrom(eventType.getRawClass());
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

}
