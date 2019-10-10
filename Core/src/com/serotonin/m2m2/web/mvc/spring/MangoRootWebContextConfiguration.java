/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatedMessageSource;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;

/**
 * Core Application Configuration
 * @author Terry Packer
 */
@Configuration
@Import({MangoCommonConfiguration.class, MangoSecurityConfiguration.class})
@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.spring.components"})
public class MangoRootWebContextConfiguration {

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
        commonsMultipartResolver.setResolveLazily(true); //So we can optionally stream the results
        commonsMultipartResolver.setDefaultEncoding(StandardCharsets.UTF_8.name());
        commonsMultipartResolver.setMaxUploadSize(Common.envProps.getLong("web.fileUpload.maxSize", 50000000));
        return commonsMultipartResolver;
    }
}
