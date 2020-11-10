/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.infiniteautomation.mango.webapp.WebAppPackage;
import com.infiniteautomation.mango.webapp.session.MangoCachingSessionDataStore;
import com.infiniteautomation.mango.webapp.session.MangoJdbcSessionDataStore;
import com.infiniteautomation.mango.webapp.session.MangoSessionDataMap;
import com.infiniteautomation.mango.webapp.session.MangoSessionDataStore;
import com.infiniteautomation.mango.webapp.session.NullMangoSessionDataStore;
import com.serotonin.m2m2.i18n.TranslatedMessageSource;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;

/**
 * Core Application Configuration
 * @author Terry Packer
 */
@Configuration
@Import({MangoCommonConfiguration.class, MangoSecurityConfiguration.class})
@ComponentScan(basePackageClasses = {WebAppPackage.class})
public class MangoRootWebContextConfiguration {
    public static final String CONTEXT_ID = "rootWebContext";

    @Bean(name="localeResolver")
    public MangoLocaleResolver getSessionLocaleResolver() {
        return new MangoLocaleResolver();
    }

    @Bean(name="messageSource")
    public TranslatedMessageSource getTranslatedMessageSource(){
        return new TranslatedMessageSource();
    }

    @Bean
    public CommonsMultipartResolver multipartResolver(@Value("${web.fileUpload.maxSize:50000000}") long fileUploadMaxSize) {
        CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
        commonsMultipartResolver.setResolveLazily(true); //So we can optionally stream the results
        commonsMultipartResolver.setDefaultEncoding(StandardCharsets.UTF_8.name());
        commonsMultipartResolver.setMaxUploadSize(fileUploadMaxSize);
        return commonsMultipartResolver;
    }

    @Bean
    @Primary
    public MangoSessionDataStore sessionDataStore(@Value("${sessionCookie.persistent:true}") boolean persistentSessions,
                                                  ApplicationContext applicationContext) {
        if (persistentSessions) {
            MangoSessionDataStore delegate = applicationContext.getBean(MangoJdbcSessionDataStore.class);
            return new MangoCachingSessionDataStore(new MangoSessionDataMap(), delegate);
        } else {
            return new NullMangoSessionDataStore();
        }
    }
}
