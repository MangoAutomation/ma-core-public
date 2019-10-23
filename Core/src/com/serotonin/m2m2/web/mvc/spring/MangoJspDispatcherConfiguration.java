/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.serotonin.m2m2.web.mvc.interceptor.CommonDataInterceptor;
import com.serotonin.m2m2.web.mvc.spring.security.MangoMethodSecurityConfiguration;
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
@Import({MangoCommonConfiguration.class, MangoMethodSecurityConfiguration.class})
@EnableWebMvc
@ComponentScan(basePackages = { "com.serotonin.m2m2.web.mvc.controller" })
public class MangoJspDispatcherConfiguration implements WebMvcConfigurer {
    public static final String CONTEXT_ID = "jspDispatcherContext";
    public static final String DISPATCHER_NAME = "JSP_DISPATCHER";


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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(commonDataInterceptor());
    }
}
