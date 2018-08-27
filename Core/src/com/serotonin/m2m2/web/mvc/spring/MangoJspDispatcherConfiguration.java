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

import com.serotonin.m2m2.web.mvc.BlabberUrlHandlerMapping;
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
@Configuration
@Import({MangoCommonConfiguration.class, MangoMethodSecurityConfiguration.class})
@EnableWebMvc
@ComponentScan(basePackages = { "com.serotonin.m2m2.web.mvc.controller" })
public class MangoJspDispatcherConfiguration implements WebMvcConfigurer {

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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(commonDataInterceptor());
    }
}
