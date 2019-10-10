/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.webapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

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
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.DwrConversionDefinition;
import com.serotonin.m2m2.module.DwrDefinition;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.web.dwr.ModuleDwr;
import com.serotonin.m2m2.web.dwr.StartupDwr;
import com.serotonin.m2m2.web.dwr.util.BlabberBeanConverter;
import com.serotonin.m2m2.web.dwr.util.BlabberConverterManager;
import com.serotonin.m2m2.web.dwr.util.DwrClassConversion;
import com.serotonin.m2m2.web.dwr.util.ModuleDwrCreator;

/**
 * @author Jared Wiltshire
 */
public final class MangoDwrServlet extends DwrServlet {
    private static final long serialVersionUID = 1L;

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        ServletContext context = servletConfig.getServletContext();
        Container container = (Container) context.getAttribute(Container.class.getName());
        WebApplicationContext webAppContext = WebApplicationContextUtils.getRequiredWebApplicationContext(context);

        configureStartupDwr(container);
        configureDwr(container, webAppContext);
    }


    /**
     * Configure the first DWR That will poll the startup process
     *
     * @param context
     */
    private void configureStartupDwr(Container container) {
        CreatorManager creatorManager = (CreatorManager) container.getBean(CreatorManager.class.getName());

        Class<?> clazz = StartupDwr.class;
        String js = clazz.getSimpleName();

        NewCreator c = new NewCreator();
        c.setClass(clazz.getName());
        c.setScope(Creator.APPLICATION);
        c.setJavascript(js);
        try {
            creatorManager.addCreator(js, c);
        } catch (IllegalArgumentException e) {
            log.info("Duplicate definition of DWR class ignored: " + clazz.getName());
        }
    }

    private void configureDwr(Container container, ListableBeanFactory beanFactory) {
        // Register declared DWR proxy classes
        CreatorManager creatorManager = (CreatorManager) container.getBean(CreatorManager.class.getName());

        Collection<DataSourceDefinition> dsDefs = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, DataSourceDefinition.class).values();
        for (DataSourceDefinition def : dsDefs) {
            Class<?> clazz = def.getDwrClass();
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

        Collection<PublisherDefinition> pubDefs = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, PublisherDefinition.class).values();
        for (PublisherDefinition def : pubDefs) {
            Class<?> clazz = def.getDwrClass();
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

        Collection<DwrDefinition> dwrDefs = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, DwrDefinition.class).values();
        for (DwrDefinition def : dwrDefs) {
            Class<? extends ModuleDwr> clazz = def.getDwrClass();
            if (clazz != null) {
                String js = clazz.getSimpleName();

                ModuleDwrCreator c = new ModuleDwrCreator(def.getModule());
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

        for (DwrConversionDefinition def : beanFactory.getBeansOfType(DwrConversionDefinition.class).values()) {
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