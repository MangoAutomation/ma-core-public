/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.WebApplicationInitializer;

/**
 * @author Jared Wiltshire
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Order(100)
public class JspServletInitializer extends JettyJasperInitializer implements WebApplicationInitializer {

    private final Environment env;

    @Autowired
    private JspServletInitializer(Environment env) {
        this.env = env;
    }

    @Override
    public void onStartup(ServletContext context) throws ServletException {
        // call JettyJasperInitializer onStartup() method
        super.onStartup(null, context);

        // JettyJspServlet has already been added via webdefault.xml inside the Jetty jar
        // see https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/jetty-webapp/src/main/config/etc/webdefault.xml
        // it is set as async-supported via web.xml in ${MA_HOME}/web/WEB-INF/web.xml
        ServletRegistration holderJsp = context.getServletRegistration("jsp");

        //Get the descriptions here: http://www.eclipse.org/jetty/documentation/9.2.10.v20150310/configuring-jsp.html
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.8");
        holderJsp.setInitParameter("compilerSourceVM", "1.8");

        //Env Configurable
        holderJsp.setInitParameter("fork", env.getProperty("web.jsp.fork", "false"));
        holderJsp.setInitParameter("keepgenerated", env.getProperty("web.jsp.keepgenerated", "true"));
        holderJsp.setInitParameter("development", env.getProperty("web.jsp.development", "true"));
        holderJsp.setInitParameter("modificationTestInterval", env.getProperty("web.jsp.modificationTestInterval", "4"));
        holderJsp.setInitParameter("genStringAsCharArray", env.getProperty("web.jsp.genStringAsCharArray", "true"));
        holderJsp.setInitParameter("trimSpaces", env.getProperty("web.jsp.trimSpaces", "false"));
        holderJsp.setInitParameter("classdebuginfo", env.getProperty("web.jsp.classdebuginfo", "false"));
        holderJsp.setInitParameter("suppressSmap", env.getProperty("web.jsp.suppressSmap", "true"));

        /*
         * org.apache.jasper.compiler.AntCompiler (see below for options)
         * or
         * org.apache.jasper.compiler.JDTCompiler (no compiler options)
         */
        holderJsp.setInitParameter("compilerClassName", env.getProperty("web.jsp.compilerClassName", "org.apache.jasper.compiler.JDTCompiler"));
        /*
         * Compiler Options for AntCompiler
         * classic - legacy JDK compiler for older 1.1/1.2 java versions
         * modern - Standard JDK Compiler
         * extJavac - JDK Compiler in JVM of its own
         * jikes - Jikes compiler
         * kjc - kopi compiler
         * gcj - gcj compiler from gcc
         */
        holderJsp.setInitParameter("compiler", env.getProperty("web.jsp.compiler", "modern"));
    }
}