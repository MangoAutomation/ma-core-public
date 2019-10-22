/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.module.ControllerMappingDefinition;
import com.serotonin.m2m2.module.HandlerInterceptorDefinition;
import com.serotonin.m2m2.module.UriMappingDefinition;
import com.serotonin.m2m2.module.UrlMappingDefinition;
import com.serotonin.m2m2.web.mvc.BlabberUrlHandlerMapping;
import com.serotonin.m2m2.web.mvc.UrlHandler;
import com.serotonin.m2m2.web.mvc.UrlHandlerController;
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

    private final ListableBeanFactory beanFactory;

    @Autowired
    public MangoJspDispatcherConfiguration(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
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
    public BlabberUrlHandlerMapping blabberUrlHandlerMapping(CommonDataInterceptor commonDataInterceptor) {
        BlabberUrlHandlerMapping mapping = new BlabberUrlHandlerMapping();
        mapping.addInterceptor(commonDataInterceptor);

        // Add interceptors
        for (HandlerInterceptorDefinition def : MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, HandlerInterceptorDefinition.class)) {
            mapping.addInterceptor(def.getInterceptor());
        }
        mapping.initInterceptors();

        for (UriMappingDefinition def : MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, UriMappingDefinition.class)) {

            String modulePath;
            if (def.getModule() == null) // Core mapping
                modulePath = "/";
            else
                modulePath = "/" + Constants.DIR_MODULES + "/" + def.getModule().getName();

            String viewName = null;
            if (def.getJspPath() != null)
                viewName = modulePath + "/" + def.getJspPath();

            UrlHandler handler = def.getHandler();
            Controller controller = new UrlHandlerController(handler, modulePath, viewName);

            mapping.registerHandler(def.getPath(), controller);
        }

        //Add The ControllerMappingDefinitions
        for(ControllerMappingDefinition def: MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, ControllerMappingDefinition.class)) {
            mapping.registerHandler(def.getPath(), def.getController());
        }

        // Add url mappings, TODO Remove this when we finally get rid of UrlMappingDefinition
        for (UrlMappingDefinition def : MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, UrlMappingDefinition.class)) {
            String modulePath = "/" + Constants.DIR_MODULES + "/" + def.getModule().getName();
            String viewName = null;
            if (def.getJspPath() != null)
                viewName = modulePath + "/" + def.getJspPath();

            UrlHandler handler = def.getHandler();
            Controller controller = new UrlHandlerController(handler, modulePath, viewName);

            mapping.registerHandler(def.getUrlPath(), controller);
        }

        return mapping;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(commonDataInterceptor());
    }
}
