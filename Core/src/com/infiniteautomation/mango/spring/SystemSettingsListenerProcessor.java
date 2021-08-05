/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.core.Ordered;

import com.serotonin.m2m2.module.SystemSettingsListenerDefinition;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsEventDispatcher;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 * Automatically adds SystemSettingsListeners to the SystemSettingsEventDispatcher (and removes them on destroy).
 * @author Jared Wiltshire
 */
public class SystemSettingsListenerProcessor implements DestructionAwareBeanPostProcessor, BeanFactoryAware, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(SystemSettingsListenerProcessor.class);

    private BeanFactory beanFactory;
    private SystemSettingsEventDispatcher dispatcher;

    /**
     * Beans which extend SystemSettingsListenerDefinition will be added to the dispatcher twice but it wont matter as the dispatcher uses a Set
     * (Lifecyle calls {@link SystemSettingsListenerDefinition#registerListener()}).
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SystemSettingsListener) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Adding system settings listener for " + beanName);
            }
            this.ensureDispatcher();
            this.dispatcher.addListener((SystemSettingsListener) bean);
        }
        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (bean instanceof SystemSettingsListener) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Removing system settings listener for " + beanName);
            }
            this.ensureDispatcher();
            this.dispatcher.removeListener((SystemSettingsListener) bean);
        }
    }

    @Override
    public boolean requiresDestruction(Object bean) {
        return bean instanceof SystemSettingsListener;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private void ensureDispatcher() {
        if (this.dispatcher == null) {
            this.dispatcher = this.beanFactory.getBean(SystemSettingsEventDispatcher.class);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
