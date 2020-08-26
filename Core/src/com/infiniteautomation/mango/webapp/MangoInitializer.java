/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionListener;
import org.eclipse.jetty.server.session.SessionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;

/**
 * @author Jared Wiltshire
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Order(0)
public class MangoInitializer implements WebApplicationInitializer {
    private final Environment env;
    private final MangoSessionListener sessionListener;

    @Autowired
    private MangoInitializer(Environment env, MangoSessionListener sessionListener) {
        this.env = env;
        this.sessionListener = sessionListener;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        this.configureSessions(servletContext);
    }

    private void configureSessions(ServletContext ctx) {
        // Disable the JSESSIONID URL Parameter, there is no option for this in SessionCookieConfig so set via init param
        ctx.setInitParameter(SessionHandler.__SessionIdPathParameterNameProperty, "none");

        SessionCookieConfig sessionCookieConfig = ctx.getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        sessionCookieConfig.setName(Common.getCookieName());

        String cookieDomain = env.getProperty("sessionCookie.domain");
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            sessionCookieConfig.setDomain(cookieDomain);
        }

        sessionCookieConfig.setMaxAge(this.sessionListener.getTimeoutSeconds());

        // Use our own MangoSessionListener instead of HttpSessionEventPublisher as there is a bug in Spring which prevents getting the Authentication from the session attribute
        // with an asynchronous event publisher
        ctx.addListener(this.sessionListener);
    }
}
