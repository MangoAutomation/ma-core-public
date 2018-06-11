/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatedMessageSource;

/**
 * Core Application Configuration
 * @author Terry Packer
 */
@Configuration
@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.spring.components"})
public class MangoApplicationContextConfiguration {

    @Bean(name="localeResolver")
    public SessionLocaleResolver getSessionLocaleResolver(){
        return new SessionLocaleResolver();
    }

    @Bean(name="messageSource")
    public TranslatedMessageSource getTranslatedMessageSource(){
        return new TranslatedMessageSource();
    }

    /**
     * J.W. This is used to convert properties from strings into lists of integers etc when they are injected by Spring
     * @return
     */
    @Bean
    public static ConversionService conversionService() {
        return new DefaultConversionService();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ConfigurableEnvironment env) {
        env.getPropertySources().addLast(new MangoPropertySource("envProps", Common.envProps));
        env.setConversionService((ConfigurableConversionService) conversionService());

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

}
