/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

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

}
