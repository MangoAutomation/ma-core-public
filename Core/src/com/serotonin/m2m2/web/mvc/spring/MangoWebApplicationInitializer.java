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

import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.swagger.SwaggerConfig;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionListener;

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
    // TODO remove this and its getter in Common
    public static final String DISPATCHER_CONTEXT_ID = "dispatcherContext";

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException {

        // Create the 'root' Spring application context
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.setId(ROOT_CONTEXT_ID);
        rootContext.setParent(Common.getRuntimeContext());
        rootContext.register(MangoApplicationContextConfiguration.class);
        rootContext.register(MangoSecurityConfiguration.class);

        // Manage the lifecycle of the root application context
        context.addListener(new ContextLoaderListener(rootContext));

        // TODO Mango 3.5 verify that it works without this context
        // Create the dispatcher servlet's Spring application context
        //AnnotationConfigWebApplicationContext dispatcherContext = new AnnotationConfigWebApplicationContext();
        //dispatcherContext.setId(DISPATCHER_CONTEXT_ID);
        //dispatcherContext.setParent(rootContext); //Setting this but it seems to get set elsewhere to the same value

        // TODO Mango 3.5 merge this config and remove common dispatcher config
        rootContext.register(MangoCoreSpringConfiguration.class);

        boolean enableRest = Common.envProps.getBoolean("rest.enabled", false);
        boolean enableSwagger = Common.envProps.getBoolean("swagger.enabled", false);

        if(enableRest){
            rootContext.register(MangoRestSpringConfiguration.class);
            rootContext.register(MangoWebSocketConfiguration.class);
        }

        if(enableSwagger&&enableRest){
            rootContext.register(SwaggerConfig.class);
        }

        // Register and map the dispatcher servlet
        ServletRegistration.Dynamic dispatcher =
                context.addServlet("springDispatcher", new DispatcherServlet(rootContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("*.htm", "*.shtm", "/rest/*",
                "/swagger/v2/api-docs",
                "/swagger-resources/configuration/ui",
                "/swagger-resources/configuration/security",
                "/swagger-resources"
                );

        //Setup the Session Listener to Help the MangoSessionRegistry know when users login/out
        context.addListener(HttpSessionEventPublisher.class);
        context.addListener(new MangoSessionListener());

    }
}
