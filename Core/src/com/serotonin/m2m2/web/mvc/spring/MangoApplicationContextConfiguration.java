/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.infiniteautomation.mango.spring.eventMulticaster.EventMulticasterRegistry;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEventMulticaster;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatedMessageSource;

/**
 * Core Application Configuration
 * @author Terry Packer
 */
@Configuration
@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.spring.components"})
public class MangoApplicationContextConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ConfigurableEnvironment env, ConfigurableConversionService conversionService, MangoPropertySource mangoPropertySource) {
        env.getPropertySources().addLast(mangoPropertySource);
        env.setConversionService(conversionService);

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

    @Bean(name="localeResolver")
    public SessionLocaleResolver getSessionLocaleResolver(){
        return new SessionLocaleResolver();
    }

    @Bean(name="messageSource")
    public TranslatedMessageSource getTranslatedMessageSource(){
        return new TranslatedMessageSource();
    }

    @Bean
    public CommonsMultipartResolver multipartResolver(){
        CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
        commonsMultipartResolver.setDefaultEncoding("utf-8");
        commonsMultipartResolver.setMaxUploadSize(Common.envProps.getLong("web.fileUpload.maxSize", 50000000));
        return commonsMultipartResolver;
    }

    @Bean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    public ApplicationEventMulticaster eventMulticaster(ApplicationContext context, EventMulticasterRegistry eventMulticasterRegistry) {
        return new PropagatingEventMulticaster(context, eventMulticasterRegistry);
    }

}
