/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.swagger.SwaggerConfig;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionListener;

/**
 *
 * <p>Class to hook into the Web Application Initialization process, creates the Spring Application contexts.</p>
 *
 * <p>We use AnnotationConfigWebApplicationContexts to perform configuration that previously was only able to be done via XML.</p>
 *
 * Context hierarchy looks like this:
 * <pre>
 * runtimeContext -> rootWebContext -> jspDispatcherContext
 *                                  -> restDispatcherContext
 * </pre>
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class MangoWebApplicationInitializer implements ServletContainerInitializer {

    public static final String RUNTIME_CONTEXT_ID = "runtimeContext";
    public static final String ROOT_WEB_CONTEXT_ID = "rootWebContext";
    public static final String JSP_DISPATCHER_CONTEXT = "jspDispatcherContext";
    public static final String REST_DISPATCHER_CONTEXT = "restDispatcherContext";

    public static final String JSP_DISPATCHER_NAME = "JSP_DISPATCHER";
    public static final String REST_DISPATCHER_NAME = "REST_DISPATCHER";

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException {

        /**
         * Root web application context configuration
         */

        // Create the Spring 'root' web application context
        AnnotationConfigWebApplicationContext rootWebContext = new AnnotationConfigWebApplicationContext();
        rootWebContext.setId(ROOT_WEB_CONTEXT_ID);
        rootWebContext.setParent(MangoRuntimeContextConfiguration.getRuntimeContext());
        rootWebContext.register(MangoApplicationContextConfiguration.class);
        rootWebContext.register(MangoSecurityConfiguration.class);

        // Manage the lifecycle of the root application context
        context.addListener(new ContextLoaderListener(rootWebContext));

        /**
         * JSP dispatcher application context configuration
         */

        // Create the dispatcher servlet's Spring application context
        AnnotationConfigWebApplicationContext jspDispatcherContext = new AnnotationConfigWebApplicationContext();
        jspDispatcherContext.setId(JSP_DISPATCHER_CONTEXT);
        jspDispatcherContext.setParent(rootWebContext);
        jspDispatcherContext.register(MangoCoreSpringConfiguration.class);

        // Register and map the JSP dispatcher servlet
        ServletRegistration.Dynamic jspDispatcher =
                context.addServlet(JSP_DISPATCHER_NAME, new DispatcherServlet(jspDispatcherContext));
        jspDispatcher.setLoadOnStartup(1);
        jspDispatcher.addMapping("*.htm", "*.shtm");

        /**
         * REST dispatcher application context configuration
         */

        boolean enableRest = Common.envProps.getBoolean("rest.enabled", false);
        boolean enableSwagger = Common.envProps.getBoolean("swagger.enabled", false);

        if (enableRest) {
            // Create the dispatcher servlet's Spring application context
            AnnotationConfigWebApplicationContext restDispatcherContext = new AnnotationConfigWebApplicationContext();
            restDispatcherContext.setId(REST_DISPATCHER_CONTEXT);
            restDispatcherContext.setParent(rootWebContext);

            restDispatcherContext.register(MangoRestSpringConfiguration.class);
            restDispatcherContext.register(MangoWebSocketConfiguration.class);

            if (enableSwagger) {
                restDispatcherContext.register(SwaggerConfig.class);
            }

            // Register and map the REST dispatcher servlet
            ServletRegistration.Dynamic restDispatcher =
                    context.addServlet(REST_DISPATCHER_NAME, new DispatcherServlet(restDispatcherContext));
            restDispatcher.setLoadOnStartup(2);
            restDispatcher.addMapping("/rest/*");

            if (enableSwagger) {
                restDispatcher.addMapping(
                        "/swagger/v2/api-docs",
                        "/swagger-resources/configuration/ui",
                        "/swagger-resources/configuration/security",
                        "/swagger-resources");
            }
        }

        // MangoSessionListener now publishes the events as there is a bug in Spring which prevents getting the Authentication from the session attribute
        //context.addListener(HttpSessionEventPublisher.class);

        // sets the session timeout
        context.addListener(new MangoSessionListener());
    }
}
