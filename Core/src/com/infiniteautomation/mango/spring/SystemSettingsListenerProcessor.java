/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;

import com.serotonin.m2m2.vo.systemSettings.SystemSettingsEventDispatcher;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 * Automatically adds SystemSettingsListeners to the SystemSettingsEventDispatcher (and removes them on destroy).
 * @author Jared Wiltshire
 */
public class SystemSettingsListenerProcessor implements DestructionAwareBeanPostProcessor {

    private final SystemSettingsEventDispatcher dispatcher;

    public SystemSettingsListenerProcessor(SystemSettingsEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Beans which extend SystemSettingsListenerDefinition will be added to the dispatcher twice but it wont matter as the dispatcher uses a Set
     * (Lifecyle calls {@link com.serotonin.m2m2.module.SystemSettingsListenerDefinition#registerListener()}).
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SystemSettingsListener) {
            this.dispatcher.addListener((SystemSettingsListener) bean);
        }
        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (bean instanceof SystemSettingsListener) {
            this.dispatcher.removeListener((SystemSettingsListener) bean);
        }
    }

    @Override
    public boolean requiresDestruction(Object bean) {
        return bean instanceof SystemSettingsListener;
    }
}
