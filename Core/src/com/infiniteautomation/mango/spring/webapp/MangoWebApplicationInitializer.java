/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.webapp;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.infiniteautomation.mango.rest.RootRestDispatcherConfiguration;
import com.infiniteautomation.mango.rest.swagger.RootSwaggerConfig;
import com.serotonin.m2m2.module.ApplicationContextDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.spring.MangoJspDispatcherConfiguration;
import com.serotonin.m2m2.web.mvc.spring.MangoRootWebContextConfiguration;
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
 *                                  -> rootRestDispatcherContext -> restv1DispatcherContext
 *                                                               -> restv2DispatcherContext
 * </pre>
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MangoWebApplicationInitializer implements ServletContainerInitializer {

    public static final String RUNTIME_CONTEXT_ID = "runtimeContext";
    public static final String ROOT_WEB_CONTEXT_ID = "rootWebContext";
    public static final String JSP_DISPATCHER_CONTEXT = "jspDispatcherContext";
    public static final String ROOT_REST_DISPATCHER_CONTEXT = "rootRestDispatcherContext";
    public static final String REST_DISPATCHER_CONTEXT = "restDispatcherContext";

    public static final String JSP_DISPATCHER_NAME = "JSP_DISPATCHER";
    public static final String ROOT_REST_DISPATCHER_NAME = "ROOT_REST_DISPATCHER";
    public static final String REST_DISPATCHER_NAME = "REST_DISPATCHER";

    private final ApplicationContext parent;
    private final Environment env;

    @Autowired
    private MangoWebApplicationInitializer(ApplicationContext parent, Environment env) {
        this.parent = parent;
        this.env = env;
    }

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException {
        /**
         * Root web application context configuration
         */

        // Create the Spring 'root' web application context
        AnnotationConfigWebApplicationContext rootWebContext = new AnnotationConfigWebApplicationContext();
        rootWebContext.setId(ROOT_WEB_CONTEXT_ID);
        rootWebContext.setParent(this.parent);
        rootWebContext.register(MangoRootWebContextConfiguration.class);
        rootWebContext.setServletContext(context);
        rootWebContext.refresh();

        // find any WebApplicationInitializer in the web context configuration and call them during startup (before initialization) so
        // they have an opportunity to add filters / servlets
        for (WebApplicationInitializer initializer : rootWebContext.getBeansOfType(WebApplicationInitializer.class).values()) {
            initializer.onStartup(context);
        }

        // Manage the lifecycle of the root application context
        context.addListener(new ContextLoaderListener(rootWebContext));

        /**
         * JSP dispatcher application context configuration
         */

        // Create the dispatcher servlet's Spring application context
        AnnotationConfigWebApplicationContext jspDispatcherContext = new AnnotationConfigWebApplicationContext();
        jspDispatcherContext.setId(JSP_DISPATCHER_CONTEXT);
        jspDispatcherContext.setParent(rootWebContext);
        jspDispatcherContext.register(MangoJspDispatcherConfiguration.class);

        // Register and map the JSP dispatcher servlet
        ServletRegistration.Dynamic jspDispatcher = context.addServlet(JSP_DISPATCHER_NAME, new DispatcherServlet(jspDispatcherContext));
        jspDispatcher.setLoadOnStartup(1);
        jspDispatcher.addMapping("*.htm", "*.shtm");

        /**
         * REST dispatcher application context configuration
         */
        boolean enableRest = env.getProperty("rest.enabled", Boolean.class, false);
        boolean enableSwagger = env.getProperty("swagger.enabled", Boolean.class, false);

        if (enableRest) {

            //The REST configuration has a parent context fro which all versions of the API
            // are children. This root rest context is defined here:
            AnnotationConfigWebApplicationContext rootRestContext = new AnnotationConfigWebApplicationContext();
            rootRestContext.setId(ROOT_REST_DISPATCHER_CONTEXT);
            rootRestContext.setParent(rootWebContext);
            rootRestContext.register(RootRestDispatcherConfiguration.class);

            if (enableSwagger) {
                rootRestContext.register(RootSwaggerConfig.class);
            }

            // Register and map the REST dispatcher servlet
            ServletRegistration.Dynamic rootRestDispatcher =
                    context.addServlet(ROOT_REST_DISPATCHER_NAME, new DispatcherServlet(rootRestContext));
            rootRestDispatcher.setLoadOnStartup(2);
            rootRestDispatcher.addMapping("/rest/*");


            // Allow modules to define dispatcher contexts
            for(ApplicationContextDefinition appContextDefinition : ModuleRegistry.getDefinitions(ApplicationContextDefinition.class)){
                appContextDefinition.configure(context, rootWebContext, rootRestContext);
            }

            if (enableSwagger) {
                rootRestDispatcher.addMapping(
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
