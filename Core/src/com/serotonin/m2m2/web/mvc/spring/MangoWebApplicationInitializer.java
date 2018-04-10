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

/**
 *
 * Class to hook into the Web Application Initialization process
 * to perform configuration that previously was only able to be done via XML.
 *
 *
 * @author Terry Packer
 */
public class MangoWebApplicationInitializer implements ServletContainerInitializer {

    private MangoWebApplicationInitializerListener listener;
    private AnnotationConfigWebApplicationContext rootContext;
    private AnnotationConfigWebApplicationContext dispatcherContext;

    public static interface MangoWebApplicationInitializerListener {
        void onStartup(AnnotationConfigWebApplicationContext rootContext, AnnotationConfigWebApplicationContext dispatcherContext);
    }

    public MangoWebApplicationInitializer(MangoWebApplicationInitializerListener listener) {
        this.listener = listener;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)
     */
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException {

        // Create the 'root' Spring application context
        rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(MangoApplicationContextConfiguration.class);
        rootContext.register(MangoSecurityConfiguration.class);

        // Manage the lifecycle of the root application context
        context.addListener(new ContextLoaderListener(rootContext));

        // Create the dispatcher servlet's Spring application context
        dispatcherContext = new AnnotationConfigWebApplicationContext();
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

        if (listener != null) {
            listener.onStartup(rootContext, dispatcherContext);
        }
    }

}
