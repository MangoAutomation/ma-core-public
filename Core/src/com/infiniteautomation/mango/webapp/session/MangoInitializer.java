/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.session;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.WebApplicationInitializer;

import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;
import com.serotonin.m2m2.web.handler.MangoErrorHandler;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionListener;

/**
 * Configures session settings for the ServletContext / Jetty WebAppContext.
 *
 * @author Jared Wiltshire
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Order(0)
public class MangoInitializer implements WebApplicationInitializer {
    private final MangoSessionListener sessionListener;
    private final MangoSessionHandler sessionHandler;
    private final PageResolver pageResolver;

    @Autowired
    private MangoInitializer(MangoSessionListener sessionListener,
                             MangoSessionHandler sessionHandler,
                             PageResolver pageResolver) {
        this.sessionListener = sessionListener;
        this.sessionHandler = sessionHandler;
        this.pageResolver = pageResolver;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        // we need to customize the underlying Jetty SessionHandler, no Servlet way of doing this
        if (servletContext instanceof ContextHandler.Context) {
            ContextHandler contextHandler = ((ContextHandler.Context) servletContext).getContextHandler();
            if (contextHandler instanceof WebAppContext) {
                configureWebAppContext((WebAppContext) contextHandler);
            }
        }

        configureServletContext(servletContext);
    }

    private void configureWebAppContext(WebAppContext context) {
        context.setSessionHandler(sessionHandler);

        //Setup error handling
        context.setErrorHandler(new MangoErrorHandler(pageResolver));
    }

    /**
     * Note:
     * {@link MangoSessionHandler.MangoSessionCookieConfig MangoSessionCookieConfig}
     * can now dynamically override the session cookie settings.
     */
    private void configureServletContext(ServletContext ctx) {
        // Use our own MangoSessionListener instead of HttpSessionEventPublisher as there is a bug in Spring which
        // prevents getting the Authentication from the session attribute with an asynchronous event publisher
        ctx.addListener(sessionListener);
    }
}
