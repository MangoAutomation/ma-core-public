/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.support.JstlUtils;

/**
 * Currently used to ensure that translations are available for all JSPs under the exception dir.
 * 
 * @author Matthew Lohbihler
 */
public class TranslationsFilter implements Filter {
    private String messageSourceKey = "messageSource";
    private ServletContext servletContext;

    @Override
    public void init(FilterConfig config) throws ServletException {
        String key = config.getInitParameter("messageSourceKey");
        if (key != null)
            messageSourceKey = key;
        servletContext = config.getServletContext();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        MessageSource messageSource = (MessageSource) wac.getBean(messageSourceKey);
        JstlUtils.exposeLocalizationContext((HttpServletRequest) request, messageSource);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // no op
    }
}
