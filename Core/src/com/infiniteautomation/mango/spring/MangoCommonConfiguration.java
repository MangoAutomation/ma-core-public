/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.infiniteautomation.mango.spring.eventMulticaster.EventMulticasterRegistry;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEventMulticaster;

/**
 * Beans which are instantiated per configuration (i.e. cannot be shared) but are configured the same.
 *
 * @author Jared Wiltshire
 */
@Configuration
@EnableScheduling
@EnableAsync
public class MangoCommonConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ConfigurableApplicationContext ctx, ConfigurableEnvironment env, ConfigurableConversionService conversionService) {
        env.setConversionService(conversionService);

        // have to manually set this now, doesn't pick up the bean defined in MangoRuntimeContextConfiguration
        ctx.getBeanFactory().setConversionService(conversionService);

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

    @Bean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    public ApplicationEventMulticaster eventMulticaster(ApplicationContext context, EventMulticasterRegistry eventMulticasterRegistry) {
        return new PropagatingEventMulticaster(context, eventMulticasterRegistry, ForkJoinPool.commonPool());
    }

    /**
     * Calls getBeansOfType() then sorts and returns as list
     * @param lbf
     * @param type
     * @return
     */
    public static <T> List<T> beansOfType(ListableBeanFactory lbf, Class<T> type) {
        List<T> beans = new ArrayList<>(lbf.getBeansOfType(type).values());
        Collections.sort(beans, AnnotationAwareOrderComparator.INSTANCE);
        return beans;
    }

    /**
     * Calls BeanFactoryUtils.beansOfTypeIncludingAncestors() then sorts and returns as list
     * @param lbf
     * @param type
     * @return
     */
    public static <T> List<T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type) {
        List<T> beans = new ArrayList<>(BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, type).values());
        Collections.sort(beans, AnnotationAwareOrderComparator.INSTANCE);
        return beans;
    }

    /**
     * BeanPostProcessor are per application context.
     * @param dispatcher
     * @return
     */
    @Bean
    public SystemSettingsListenerProcessor systemSettingsListenerProcessor() {
        return new SystemSettingsListenerProcessor();
    }
}
