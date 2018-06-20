/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.ApplicationListener;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.swagger.SwaggerConfig;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;

/**
 *
 * Class to hook into the Web Application Initialization process
 * to perform configuration that previously was only able to be done via XML.
 *
 *
 * @author Terry Packer
 */
public class MangoWebApplicationInitializer implements ServletContainerInitializer {

    public static final String ROOT_CONTEXT_ID = "rootContext";
    public static final String DISPATCHER_CONTEXT_ID = "dispatcherContext";
    private ApplicationListener<?> contextListener;

    public MangoWebApplicationInitializer(ApplicationListener<?> contextListener) {
        this.contextListener = contextListener;
    }

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException {

        // Create the 'root' Spring application context
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.setId(ROOT_CONTEXT_ID);
        rootContext.register(MangoApplicationContextConfiguration.class);
        rootContext.register(MangoSecurityConfiguration.class);

        // Manage the lifecycle of the root application context
        context.addListener(new ContextLoaderListener(rootContext));

        // Create the dispatcher servlet's Spring application context
        AnnotationConfigWebApplicationContext dispatcherContext = new AnnotationConfigWebApplicationContext();
        dispatcherContext.setId(DISPATCHER_CONTEXT_ID);
        dispatcherContext.register(MangoCoreSpringConfiguration.class);

        boolean enableRest = Common.envProps.getBoolean("rest.enabled", false);
        boolean enableSwagger = Common.envProps.getBoolean("swagger.enabled", false);

        if(enableRest){
            dispatcherContext.register(MangoRestSpringConfiguration.class);
            dispatcherContext.register(MangoWebSocketConfiguration.class);
        }

        if(enableSwagger&&enableRest){
            dispatcherContext.register(SwaggerConfig.class);
        }

        // Register and map the dispatcher servlet
        ServletRegistration.Dynamic dispatcher =
                context.addServlet("springDispatcher", new DispatcherServlet(dispatcherContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("*.htm", "*.shtm", "/rest/*", "/api-docs/*");

        //Setup the Session Listener to Help the MangoSessionRegistry know when users login/out
        context.addListener(HttpSessionEventPublisher.class);

        if (contextListener != null) {
            rootContext.addApplicationListener(contextListener);
        }
    }
}
