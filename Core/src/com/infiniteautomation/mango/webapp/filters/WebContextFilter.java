/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.infiniteautomation.mango.webapp.filters;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.directwebremoting.impl.DefaultWebContextBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;

/**
 * @author Matthew Lohbihler
 */
@Component
@ConditionalOnProperty("${web.dwr.enabled:true}")
@WebFilter(
        filterName = WebContextFilter.NAME,
        urlPatterns = {"*.shtm"},
        asyncSupported = false,
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.ASYNC})
@Order(FilterOrder.CORE_FILTERS)
public class WebContextFilter implements Filter {
    public static final String NAME = "WebContext";

    private final DefaultWebContextBuilder builder = new DefaultWebContextBuilder();
    private ServletContext servletContext;

    @Override
    public void init(FilterConfig config) {
        servletContext = config.getServletContext();
    }

    @Override
    public void destroy() {
        // no op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
    ServletException {
        try {
            builder.set((HttpServletRequest) request, (HttpServletResponse) response, null, servletContext, null);
            chain.doFilter(request, response);
        }
        finally {
            builder.unset();
        }
    }
}
