/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.WebApplicationInitializer;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;

/**
 * Adds and configures the JettyJspServlet
 * @author Jared Wiltshire
 */
@Component
@ConditionalOnProperty("${web.jsp.enabled:true}")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Order(200)
public class JspServletInitializer extends JettyJasperInitializer implements WebApplicationInitializer {

    private final Environment env;

    @Autowired
    private JspServletInitializer(Environment env) {
        this.env = env;
    }

    /**
     * See <a href="https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-webapp/src/main/config/etc/webdefault.xml">webdefault.xml</a> for init parameter descriptions.
     */
    @Override
    public void onStartup(ServletContext context) throws ServletException {
        ServletRegistration.Dynamic registration = context.addServlet("jsp", JettyJspServlet.class);
        registration.setAsyncSupported(true);
        registration.setLoadOnStartup(0);
        registration.addMapping("*.jsp", "*.jspf", "*.jspx", "*.xsp", "*.JSP", "*.JSPF", "*.JSPX", "*.XSP");

        // Get the descriptions here:
        registration.setInitParameter("xpoweredBy", "false");
        registration.setInitParameter("compilerTargetVM", "1.8");
        registration.setInitParameter("compilerSourceVM", "1.8");
        registration.setInitParameter("logVerbosityLevel", "DEBUG");
        registration.setInitParameter("classpath", "?");

        //Env Configurable
        registration.setInitParameter("fork", env.getProperty("web.jsp.fork", "false"));
        registration.setInitParameter("keepgenerated", env.getProperty("web.jsp.keepgenerated", "true"));
        registration.setInitParameter("development", env.getProperty("web.jsp.development", "true"));
        registration.setInitParameter("modificationTestInterval", env.getProperty("web.jsp.modificationTestInterval", "4"));
        registration.setInitParameter("genStringAsCharArray", env.getProperty("web.jsp.genStringAsCharArray", "true"));
        registration.setInitParameter("trimSpaces", env.getProperty("web.jsp.trimSpaces", "false"));
        registration.setInitParameter("classdebuginfo", env.getProperty("web.jsp.classdebuginfo", "false"));
        registration.setInitParameter("suppressSmap", env.getProperty("web.jsp.suppressSmap", "true"));

        /*
         * org.apache.jasper.compiler.AntCompiler (see below for options)
         * or
         * org.apache.jasper.compiler.JDTCompiler (no compiler options)
         */
        registration.setInitParameter("compilerClassName", env.getProperty("web.jsp.compilerClassName", "org.apache.jasper.compiler.JDTCompiler"));
        /*
         * Compiler Options for AntCompiler
         * classic - legacy JDK compiler for older 1.1/1.2 java versions
         * modern - Standard JDK Compiler
         * extJavac - JDK Compiler in JVM of its own
         * jikes - Jikes compiler
         * kjc - kopi compiler
         * gcj - gcj compiler from gcc
         */
        registration.setInitParameter("compiler", env.getProperty("web.jsp.compiler", "modern"));

        // call JettyJasperInitializer onStartup() method
        super.onStartup(null, context);
    }
}