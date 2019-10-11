/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.servlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.Container;
import org.directwebremoting.create.NewCreator;
import org.directwebremoting.extend.Converter;
import org.directwebremoting.extend.ConverterManager;
import org.directwebremoting.extend.Creator;
import org.directwebremoting.extend.CreatorManager;
import org.directwebremoting.servlet.DwrServlet;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.serotonin.m2m2.module.DwrClassHolder;
import com.serotonin.m2m2.module.DwrConversionDefinition;
import com.serotonin.m2m2.web.dwr.StartupDwr;
import com.serotonin.m2m2.web.dwr.util.BlabberBeanConverter;
import com.serotonin.m2m2.web.dwr.util.BlabberConverterManager;
import com.serotonin.m2m2.web.dwr.util.DwrClassConversion;

/**
 * @author Jared Wiltshire
 */

@WebServlet(
        name = MangoDwrServlet.NAME,
        loadOnStartup = 2,
        urlPatterns = {"/dwr/*"},
        initParams = {
                @WebInitParam(name = "activeReverseAjaxEnabled", value = "false"),
                @WebInitParam(name = "publishContainerAs", value = "DwrContainer"),
                @WebInitParam(name = "crossDomainSessionSecurity", value = "true"),
                @WebInitParam(name = "allowScriptTagRemoting", value = "false"),
                @WebInitParam(name = "org.directwebremoting.extend.ConverterManager", value = "com.serotonin.m2m2.web.dwr.util.BlabberConverterManager"),
                @WebInitParam(name = "sessionCookieName", value = "XSRF-TOKEN")
        })
@Component
public final class MangoDwrServlet extends DwrServlet {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "dwr-invoker";

    private final Log log = LogFactory.getLog(this.getClass());

    private final ListableBeanFactory beanFactory;

    @Autowired
    private MangoDwrServlet(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        ServletContext context = servletConfig.getServletContext();
        Container container = (Container) context.getAttribute(Container.class.getName());
        configureDwr(container);
    }

    private void configureDwr(Container container) {
        // Register declared DWR proxy classes
        CreatorManager creatorManager = (CreatorManager) container.getBean(CreatorManager.class.getName());

        Set<Class<?>> classes = new HashSet<>();
        classes.add(StartupDwr.class);

        MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, DwrClassHolder.class).stream()
        .forEach(def -> classes.add(def.getDwrClass()));

        for (Class<?> clazz : classes) {
            if (clazz != null) {
                String js = clazz.getSimpleName();

                NewCreator c = new NewCreator();
                c.setClass(clazz.getName());
                c.setScope(Creator.APPLICATION);
                c.setJavascript(js);
                try {
                    creatorManager.addCreator(js, c);
                    log.info("Added DWR definition for: " + js);
                } catch (IllegalArgumentException e) {
                    log.info("Duplicate definition of DWR class ignored: " + clazz.getName());
                }
            }
        }

        BlabberConverterManager converterManager = (BlabberConverterManager) container.getBean(ConverterManager.class
                .getName());

        for (DwrConversionDefinition def : MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, DwrConversionDefinition.class)) {
            for (DwrClassConversion conversion : def.getConversions()) {
                try {
                    Map<String, String> params = new HashMap<>();
                    //Add any defined parameters for the module conversion
                    Map<String, String> conversionParams = conversion.getParameters();
                    if (conversionParams != null)
                        params.putAll(conversionParams);

                    String converterType = conversion.getConverterType();

                    if ("bean".equals(converterType)) {
                        String paramKey = null;
                        List<String> cludes = new ArrayList<>();

                        // Check if there already is a converter for the class.
                        Converter converter = converterManager.getConverterAssignableFromNoAdd(conversion.getClazz());

                        // Special handling only for blabber converters
                        if (converter instanceof BlabberBeanConverter) {
                            converterType = "blabberBean";
                            BlabberBeanConverter blab = (BlabberBeanConverter) converter;

                            if (!CollectionUtils.isEmpty(blab.getExclusions()) && conversion.getIncludes() != null)
                                throw new RuntimeException("Class conversion '" + conversion.getClazz().getName()
                                        + "' cannot have inclusions because the overriden converter has exclusions");

                            if (!CollectionUtils.isEmpty(blab.getInclusions()) && conversion.getExcludes() != null)
                                throw new RuntimeException("Class conversion '" + conversion.getClazz().getName()
                                        + "' cannot have exclusions because the overriden converter has inclusions");

                            if (!CollectionUtils.isEmpty(blab.getInclusions())) {
                                paramKey = "include";
                                cludes.addAll(blab.getInclusions());
                            }
                            else if (!CollectionUtils.isEmpty(blab.getExclusions())) {
                                paramKey = "exclude";
                                cludes.addAll(blab.getExclusions());
                            }
                        }

                        if (conversion.getIncludes() != null) {
                            paramKey = "include";
                            cludes.addAll(conversion.getIncludes());
                        }
                        else if (conversion.getExcludes() != null) {
                            paramKey = "exclude";
                            cludes.addAll(conversion.getExcludes());
                        }

                        if (paramKey != null)
                            params.put(paramKey, com.serotonin.util.CollectionUtils.implode(cludes, ","));
                    }

                    converterManager.addConverter(conversion.getClazz().getName(), converterType, params);
                }
                catch (Exception e) {
                    log.error("Error adding DWR converter", e);
                }
            }
        }
    }
}