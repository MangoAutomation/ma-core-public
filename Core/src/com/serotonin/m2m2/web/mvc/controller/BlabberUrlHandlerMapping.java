/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;

import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.module.ControllerMappingDefinition;
import com.serotonin.m2m2.module.HandlerInterceptorDefinition;
import com.serotonin.m2m2.module.UriMappingDefinition;
import com.serotonin.m2m2.module.UrlMappingDefinition;
import com.serotonin.m2m2.web.mvc.UrlHandler;
import com.serotonin.m2m2.web.mvc.UrlHandlerController;
import com.serotonin.m2m2.web.mvc.interceptor.CommonDataInterceptor;

@SuppressWarnings("deprecation")
@Component("mappings")
public class BlabberUrlHandlerMapping extends SimpleUrlHandlerMapping {

    private final ListableBeanFactory beanFactory;
    private final CommonDataInterceptor commonDataInterceptor;

    @Autowired
    public BlabberUrlHandlerMapping(ListableBeanFactory beanFactory, CommonDataInterceptor commonDataInterceptor) {
        this.beanFactory = beanFactory;
        this.commonDataInterceptor = commonDataInterceptor;
    }

    @PostConstruct
    private void initialize() {
        this.setInterceptors(commonDataInterceptor);

        // Add interceptors
        for (HandlerInterceptorDefinition def : MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, HandlerInterceptorDefinition.class)) {
            this.setInterceptors(def.getInterceptor());
        }
        this.initInterceptors();

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

            this.registerHandler(def.getPath(), controller);
        }

        //Add The ControllerMappingDefinitions
        for(ControllerMappingDefinition def: MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, ControllerMappingDefinition.class)) {
            this.registerHandler(def.getPath(), def.getController());
        }

        // Add url mappings, TODO Remove this when we finally get rid of UrlMappingDefinition
        for (UrlMappingDefinition def : MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, UrlMappingDefinition.class)) {
            String modulePath = "/" + Constants.DIR_MODULES + "/" + def.getModule().getName();
            String viewName = null;
            if (def.getJspPath() != null)
                viewName = modulePath + "/" + def.getJspPath();

            UrlHandler handler = def.getHandler();
            Controller controller = new UrlHandlerController(handler, modulePath, viewName);

            this.registerHandler(def.getUrlPath(), controller);
        }
    }
}
