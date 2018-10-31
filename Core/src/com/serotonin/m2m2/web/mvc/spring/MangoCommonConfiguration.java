/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;

import com.infiniteautomation.mango.spring.eventMulticaster.EventMulticasterRegistry;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEventMulticaster;

/**
 * Beans which are instantiated per configuration (i.e. cannot be shared) but are configured the same.
 *
 * @author Jared Wiltshire
 */
@Configuration
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
        return new PropagatingEventMulticaster(context, eventMulticasterRegistry);
    }

}
