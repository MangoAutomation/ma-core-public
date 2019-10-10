/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.webapp;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.directwebremoting.extend.ConverterManager;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.web.dwr.util.BlabberConverterManager;

/**
 * @author Jared Wiltshire
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Order(400)
public class DwrServletInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException {
        ServletRegistration.Dynamic dwrHolder = context.addServlet("dwr-invoker", MangoDwrServlet.class);

        dwrHolder.setInitParameter("activeReverseAjaxEnabled", "true");
        dwrHolder.setInitParameter("publishContainerAs", "DwrContainer");
        dwrHolder.setInitParameter("crossDomainSessionSecurity", "true");
        dwrHolder.setInitParameter("allowScriptTagRemoting", "false");
        dwrHolder.setInitParameter(ConverterManager.class.getName(), BlabberConverterManager.class.getName());
        // tell DWR to send the XSRF-TOKEN cookie rather than the session cookie (which is now HTTP only and unable to be read by the DWR JavaScript)
        dwrHolder.setInitParameter("sessionCookieName", "XSRF-TOKEN");
        dwrHolder.setLoadOnStartup(2);
        dwrHolder.addMapping("/dwr/*");
    }
}
