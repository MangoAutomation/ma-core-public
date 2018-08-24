/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
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
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import com.infiniteautomation.mango.spring.eventMulticaster.EventMulticasterRegistry;
import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEventMulticaster;
import com.serotonin.m2m2.web.mvc.BlabberUrlHandlerMapping;
import com.serotonin.m2m2.web.mvc.interceptor.CommonDataInterceptor;
import com.serotonin.propertyEditor.DefaultMessageCodesResolver;

/**
 * Spring Configuration for Mango Automation Core
 *
 *
 * @author Terry Packer
 *
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = { "com.serotonin.m2m2.web.mvc.controller" })
public class MangoJspDispatcherConfiguration implements WebMvcConfigurer {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ConfigurableEnvironment env, ConfigurableConversionService conversionService, MangoPropertySource mangoPropertySource) {
        env.getPropertySources().addLast(mangoPropertySource);
        env.setConversionService(conversionService);

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

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

    @Bean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    public ApplicationEventMulticaster eventMulticaster(ApplicationContext context, EventMulticasterRegistry eventMulticasterRegistry) {
        return new PropagatingEventMulticaster(context, eventMulticasterRegistry);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(commonDataInterceptor());
    }
}
