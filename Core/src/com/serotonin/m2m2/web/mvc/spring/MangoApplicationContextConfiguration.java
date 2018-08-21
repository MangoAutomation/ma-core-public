/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
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

    final Logger log = LoggerFactory.getLogger(this.getClass());

    @Bean(name="localeResolver")
    public SessionLocaleResolver getSessionLocaleResolver(){
        return new SessionLocaleResolver();
    }

    @Bean(name="messageSource")
    public TranslatedMessageSource getTranslatedMessageSource(){
        return new TranslatedMessageSource();
    }

    @EventListener
    private void contextRefreshed(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();

        if (log.isInfoEnabled()) {
            log.info("Spring context '" + context.getId() +"' refreshed: " + context.getDisplayName());
        }

        if (MangoWebApplicationInitializer.ROOT_CONTEXT_ID.equals(context.getId())) {
            Common.setRootContext(context);
        } else if (MangoWebApplicationInitializer.DISPATCHER_CONTEXT_ID.equals(context.getId())) {
            Common.setDispatcherContext(context);
        }
    }
}
