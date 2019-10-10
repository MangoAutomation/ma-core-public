/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.webapp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.infiniteautomation.mango.rest.RootRestDispatcherConfiguration;
import com.infiniteautomation.mango.rest.swagger.RootSwaggerConfig;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ApplicationContextDefinition;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.permission.Permissions;
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
@Order(100)
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
        constantsInitialize(context);
        configureSessions(context);

        /**
         * Root web application context configuration
         */

        // Create the Spring 'root' web application context
        AnnotationConfigWebApplicationContext rootWebContext = new AnnotationConfigWebApplicationContext();
        rootWebContext.setId(ROOT_WEB_CONTEXT_ID);
        rootWebContext.setParent(this.parent);
        rootWebContext.register(MangoRootWebContextConfiguration.class);
        //rootWebContext.setServletContext(context);
        //rootWebContext.refresh();

        // find any WebApplicationInitializer in the web context configuration and call them during startup (before initialization) so
        // they have an opportunity to add filters / servlets
        //for (WebApplicationInitializer initializer : rootWebContext.getBeansOfType(WebApplicationInitializer.class).values()) {
        //    initializer.onStartup(context);
        //}

        // Manage the lifecycle of the root application context
        context.addListener(new ContextLoaderListener(rootWebContext));


        Collection<DataSourceDefinition> test = parent.getBeansOfType(DataSourceDefinition.class).values();

        /**
         * JSP dispatcher application context configuration
         */

        // Create the dispatcher servlet's Spring application context
        AnnotationConfigWebApplicationContext jspDispatcherContext = new AnnotationConfigWebApplicationContext();
        jspDispatcherContext.setId(JSP_DISPATCHER_CONTEXT);
        jspDispatcherContext.setParent(rootWebContext);
        jspDispatcherContext.register(MangoJspDispatcherConfiguration.class);

        // Register and map the JSP dispatcher servlet
        ServletRegistration.Dynamic jspDispatcher =
                context.addServlet(JSP_DISPATCHER_NAME, new DispatcherServlet(jspDispatcherContext));
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

    private void constantsInitialize(ServletContext ctx) {
        ctx.setAttribute("constants.Common.NEW_ID", Common.NEW_ID);

        ctx.setAttribute("constants.DataTypes.BINARY", DataTypes.BINARY);
        ctx.setAttribute("constants.DataTypes.MULTISTATE", DataTypes.MULTISTATE);
        ctx.setAttribute("constants.DataTypes.NUMERIC", DataTypes.NUMERIC);
        ctx.setAttribute("constants.DataTypes.ALPHANUMERIC", DataTypes.ALPHANUMERIC);
        ctx.setAttribute("constants.DataTypes.IMAGE", DataTypes.IMAGE);

        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.NONE", Permissions.DataPointAccessTypes.NONE);
        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.READ", Permissions.DataPointAccessTypes.READ);
        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.SET", Permissions.DataPointAccessTypes.SET);
        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.DATA_SOURCE",
                Permissions.DataPointAccessTypes.DATA_SOURCE);
        ctx.setAttribute("constants.Permissions.DataPointAccessTypes.ADMIN", Permissions.DataPointAccessTypes.ADMIN);

        ctx.setAttribute("constants.EventType.EventTypeNames.DATA_POINT", EventType.EventTypeNames.DATA_POINT);
        ctx.setAttribute("constants.EventType.EventTypeNames.DATA_SOURCE", EventType.EventTypeNames.DATA_SOURCE);
        ctx.setAttribute("constants.EventType.EventTypeNames.SYSTEM", EventType.EventTypeNames.SYSTEM);
        ctx.setAttribute("constants.EventType.EventTypeNames.PUBLISHER", EventType.EventTypeNames.PUBLISHER);
        ctx.setAttribute("constants.EventType.EventTypeNames.AUDIT", EventType.EventTypeNames.AUDIT);

        ctx.setAttribute("constants.SystemEventType.TYPE_SYSTEM_STARTUP", SystemEventType.TYPE_SYSTEM_STARTUP);
        ctx.setAttribute("constants.SystemEventType.TYPE_SYSTEM_SHUTDOWN", SystemEventType.TYPE_SYSTEM_SHUTDOWN);
        ctx.setAttribute("constants.SystemEventType.TYPE_MAX_ALARM_LEVEL_CHANGED",
                SystemEventType.TYPE_MAX_ALARM_LEVEL_CHANGED);
        ctx.setAttribute("constants.SystemEventType.TYPE_USER_LOGIN", SystemEventType.TYPE_USER_LOGIN);
        ctx.setAttribute("constants.SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE",
                SystemEventType.TYPE_SET_POINT_HANDLER_FAILURE);
        ctx.setAttribute("constants.SystemEventType.TYPE_EMAIL_SEND_FAILURE", SystemEventType.TYPE_EMAIL_SEND_FAILURE);
        ctx.setAttribute("constants.SystemEventType.TYPE_PROCESS_FAILURE", SystemEventType.TYPE_PROCESS_FAILURE);
        ctx.setAttribute("constants.SystemEventType.TYPE_LICENSE_CHECK", SystemEventType.TYPE_LICENSE_CHECK);
        ctx.setAttribute("constants.SystemEventType.TYPE_UPGRADE_CHECK", SystemEventType.TYPE_UPGRADE_CHECK);

        ctx.setAttribute("constants.AuditEventType.TYPE_DATA_SOURCE", AuditEventType.TYPE_DATA_SOURCE);
        ctx.setAttribute("constants.AuditEventType.TYPE_DATA_POINT", AuditEventType.TYPE_DATA_POINT);
        ctx.setAttribute("constants.AuditEventType.TYPE_EVENT_DETECTOR", AuditEventType.TYPE_EVENT_DETECTOR);
        ctx.setAttribute("constants.AuditEventType.TYPE_EVENT_HANDLER", AuditEventType.TYPE_EVENT_HANDLER);

        ctx.setAttribute("constants.UserComment.TYPE_EVENT", UserCommentVO.TYPE_EVENT);
        ctx.setAttribute("constants.UserComment.TYPE_POINT", UserCommentVO.TYPE_POINT);

        String[] codes = { "common.access.read", "common.access.set", "common.alarmLevel.none",
                "common.alarmLevel.info", "common.alarmLevel.important", "common.alarmLevel.warning", "common.alarmLevel.urgent", "common.alarmLevel.critical",
                "common.alarmLevel.lifeSafety", "common.alarmLevel.doNotLog", "common.alarmLevel.ignore", "common.disabled", "common.administrator", "common.user",
                "common.disabledToggle", "common.enabledToggle", "common.maximize", "common.minimize",
                "common.loading", "js.help.error", "js.help.related", "js.help.lastUpdated", "common.sendTestEmail",
                "js.email.noRecipients", "js.email.addMailingList", "js.email.addUser", "js.email.addAddress",
                "js.email.noRecipForEmail", "js.email.testSent", "events.silence", "events.unsilence", "header.mute",
                "header.unmute", };
        Map<String, TranslatableMessage> messages = new HashMap<>();
        for (String code : codes)
            messages.put(code, new TranslatableMessage(code));
        ctx.setAttribute("clientSideMessages", messages);

    }

    private void configureSessions(ServletContext ctx) {
        //Disable the JSESSIONID URL Parameter
        ctx.setInitParameter("org.eclipse.jetty.servlet.SessionIdPathParameterName", "none");

        SessionCookieConfig sessionCookieConfig = ctx.getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        sessionCookieConfig.setName(Common.getCookieName());

        String cookieDomain = env.getProperty("sessionCookie.domain");
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            sessionCookieConfig.setDomain(cookieDomain);
        }
    }
}
