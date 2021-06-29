/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.WebApplicationInitializer;

/**
 *
 * Adds and configures the DefaultServlet responsible for serving file resources and welcome files (index.html etc)
 *
 * @author Jared Wiltshire
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Order(100)
public class DefaultServletInitializer implements WebApplicationInitializer {

    private final Environment env;

    @Autowired
    private DefaultServletInitializer(Environment env) {
        this.env = env;
    }

    /**
     * See <a href="https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-webapp/src/main/config/etc/webdefault.xml">webdefault.xml</a> for init parameter descriptions.
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        ServletRegistration.Dynamic registration = servletContext.addServlet("default", DefaultServlet.class);
        registration.setAsyncSupported(true);
        registration.setLoadOnStartup(0);
        registration.addMapping("/");

        registration.setInitParameter("acceptRanges", "true");
        registration.setInitParameter("dirAllowed", env.getProperty("web.defaultServlet.dirAllowed", "false"));
        registration.setInitParameter("welcomeServlets", "exact");
        registration.setInitParameter("redirectWelcome", "false");
        registration.setInitParameter("maxCacheSize", env.getProperty("web.defaultServlet.maxCacheSize", "256000000"));
        registration.setInitParameter("maxCachedFileSize", env.getProperty("web.defaultServlet.maxCachedFileSize", "200000000"));
        registration.setInitParameter("maxCachedFiles", env.getProperty("web.defaultServlet.maxCachedFiles", "2048"));
        registration.setInitParameter("etags", env.getProperty("web.defaultServlet.etags", "false"));
        String useFileMappedBufferDefault = SystemUtils.IS_OS_WINDOWS ? "false" : "true";
        registration.setInitParameter("useFileMappedBuffer", env.getProperty("web.defaultServlet.useFileMappedBuffer", useFileMappedBufferDefault));
    }

}
