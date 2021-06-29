/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.WebApplicationInitializer;

import com.infiniteautomation.mango.spring.MangoCommonConfiguration;
import com.serotonin.m2m2.module.ServletDefinition;

/**
 * Responsible for registering all filters annotated with @WebFilter and servlets annotated with @WebServlet.
 * Also registers servlets from ServletDefinitions.
 *
 * @author Jared Wiltshire
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Order(300)
public class RegisterFiltersAndServlets implements WebApplicationInitializer {

    private final ListableBeanFactory beanFactory;
    private final Environment env;

    @Autowired
    private RegisterFiltersAndServlets(ListableBeanFactory beanFactory, Environment env) {
        this.beanFactory = beanFactory;
        this.env = env;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        this.registerFilters(servletContext);
        this.registerServlets(servletContext);
        this.registerServletDefinitionServlets(servletContext);
    }

    private Map<String, String> resolveInitParameters(WebInitParam[] initParams) {
        return Arrays.stream(initParams)
                .collect(Collectors.toMap(WebInitParam::name, param -> {
                    return env.resolveRequiredPlaceholders(param.value());
                }));
    }

    /**
     * Finds and adds all Filters which are annotated with @WebFilter
     * @param servletContext
     * @throws ServletException
     */
    private void registerFilters(ServletContext servletContext) throws ServletException {
        List<Object> filtersAndInitializers = new ArrayList<>();
        filtersAndInitializers.addAll(beanFactory.getBeansOfType(FilterInitializer.class).values());

        beanFactory.getBeansOfType(Filter.class).values().stream().forEach(f -> {
            if (f.getClass().getAnnotation(WebFilter.class) != null) {
                filtersAndInitializers.add(f);
            }
        });

        Collections.sort(filtersAndInitializers, AnnotationAwareOrderComparator.INSTANCE);

        for (Object filterObject : filtersAndInitializers) {
            if (filterObject instanceof FilterInitializer) {
                ((FilterInitializer) filterObject).onStartup(servletContext);
            } else if (filterObject instanceof Filter) {
                Filter filter = (Filter) filterObject;
                WebFilter annotation = filter.getClass().getAnnotation(WebFilter.class);
                String name = annotation.filterName();
                if (name == null || name.isEmpty()) {
                    name = filter.getClass().getSimpleName();
                }
                FilterRegistration.Dynamic registration = servletContext.addFilter(name, filter);
                if (registration != null) {
                    registration.setAsyncSupported(annotation.asyncSupported());
                    registration.setInitParameters(resolveInitParameters(annotation.initParams()));

                    EnumSet<DispatcherType> dispatcherTypes = Arrays.stream(annotation.dispatcherTypes())
                            .collect(Collectors.toCollection(() -> EnumSet.noneOf(DispatcherType.class)));

                    if (annotation.urlPatterns().length > 0) {
                        registration.addMappingForUrlPatterns(dispatcherTypes, true, annotation.urlPatterns());
                    }
                    if (annotation.servletNames().length > 0) {
                        registration.addMappingForServletNames(dispatcherTypes, true, annotation.servletNames());
                    }
                }
            }
        }
    }

    /**
     * Finds and adds all Servlets which are annotated with @WebServlet
     * @param servletContext
     * @throws ServletException
     */
    private void registerServlets(ServletContext servletContext) throws ServletException {
        List<Object> servletsAndInitializers = new ArrayList<>();
        servletsAndInitializers.addAll(beanFactory.getBeansOfType(ServletInitializer.class).values());

        beanFactory.getBeansOfType(Servlet.class).values().stream().forEach(f -> {
            if (f.getClass().getAnnotation(WebServlet.class) != null) {
                servletsAndInitializers.add(f);
            }
        });

        Collections.sort(servletsAndInitializers, AnnotationAwareOrderComparator.INSTANCE);

        for (Object servletObject : servletsAndInitializers) {
            if (servletObject instanceof ServletInitializer) {
                ((ServletInitializer) servletObject).onStartup(servletContext);
            } else if (servletObject instanceof Servlet) {
                Servlet servlet = (Servlet) servletObject;
                WebServlet annotation = servlet.getClass().getAnnotation(WebServlet.class);

                String name = annotation.name();
                if (name == null || name.isEmpty()) {
                    name = servlet.getClass().getSimpleName();
                }
                ServletRegistration.Dynamic registration = servletContext.addServlet(name, servlet);
                if (registration != null) {
                    registration.setAsyncSupported(annotation.asyncSupported());
                    registration.setInitParameters(resolveInitParameters(annotation.initParams()));
                    registration.setLoadOnStartup(annotation.loadOnStartup());

                    if (annotation.urlPatterns().length > 0) {
                        registration.addMapping(annotation.urlPatterns());
                    }
                }
            }
        }
    }

    private void registerServletDefinitionServlets(ServletContext servletContext) {
        List<ServletDefinition> servletDefinitions = MangoCommonConfiguration.beansOfTypeIncludingAncestors(beanFactory, ServletDefinition.class);
        for (ServletDefinition def : servletDefinitions) {
            ServletRegistration.Dynamic registration = servletContext.addServlet(def.getClass().getSimpleName(), def.getServlet());
            registration.setInitParameters(def.getInitParameters());
            registration.setLoadOnStartup(def.getInitOrder());
            registration.addMapping(def.getUriPatterns());
            registration.setAsyncSupported(def.isAsync());
        }
    }
}
