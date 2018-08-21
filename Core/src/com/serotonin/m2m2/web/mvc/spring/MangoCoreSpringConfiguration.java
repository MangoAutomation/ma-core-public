/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import com.infiniteautomation.mango.spring.eventMulticaster.EventMulticasterRegistry;
import com.infiniteautomation.mango.spring.eventMulticaster.MangoEventMulticaster;
import com.serotonin.m2m2.web.mvc.BlabberUrlHandlerMapping;
import com.serotonin.m2m2.web.mvc.interceptor.CommonDataInterceptor;
import com.serotonin.m2m2.web.mvc.spring.exception.MangoSpringExceptionHandler;
import com.serotonin.m2m2.web.mvc.spring.security.permissions.MangoMethodSecurityExpressionHandler;
import com.serotonin.m2m2.web.mvc.spring.security.permissions.MangoPermissionEvaluator;
import com.serotonin.propertyEditor.DefaultMessageCodesResolver;

/**
 * Spring Configuration for Mango Automation Core
 *
 *
 * @author Terry Packer
 *
 */
@SuppressWarnings("deprecation")
@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@EnableWebMvc
@ComponentScan(basePackages = { "com.serotonin.m2m2.web.mvc.controller" })
public class MangoCoreSpringConfiguration extends GlobalMethodSecurityConfiguration {

    @Bean(name="viewResolver")
    public InternalResourceViewResolver internalResourceViewResolver(){
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
        return resolver;
    }

    @Bean(name="defaultMessageCodeResolver")
    public DefaultMessageCodesResolver defaultMessageCodesResolver(){
        return new DefaultMessageCodesResolver();
    }

    @Bean(name="commonData")
    public CommonDataInterceptor commonDataInterceptor(){
        return new CommonDataInterceptor();
    }

    @Bean(name="mappings")
    public BlabberUrlHandlerMapping blabberUrlHandlerMapping(CommonDataInterceptor commonDataInterceptor){

        BlabberUrlHandlerMapping mapping = new BlabberUrlHandlerMapping();
        mapping.addInterceptor(commonDataInterceptor);

        return mapping;
    }

    @Bean
    public MangoSpringExceptionHandler exceptionHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher){
        return new MangoSpringExceptionHandler(browserHtmlRequestMatcher);
    }

    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        MangoMethodSecurityExpressionHandler expressionHandler = new MangoMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(new MangoPermissionEvaluator());
        return expressionHandler;
    }

    @Bean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    public ApplicationEventMulticaster eventMulticaster(EventMulticasterRegistry eventMulticasterRegistry) {
        return new MangoEventMulticaster(eventMulticasterRegistry);
    }
}
