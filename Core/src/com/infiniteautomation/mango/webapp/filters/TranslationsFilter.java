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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.JstlUtils;

/**
 * Currently used to ensure that translations are available for all JSPs under the exception dir.
 *
 * @author Matthew Lohbihler
 */
@Component
@WebFilter(
        filterName = TranslationsFilter.NAME,
        urlPatterns = {"/exception/*"},
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.FORWARD})
public class TranslationsFilter implements Filter {

    public static final String NAME = "Translations";
    private final MessageSource messageSource;

    @Autowired
    private TranslationsFilter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        JstlUtils.exposeLocalizationContext((HttpServletRequest) request, messageSource);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // no op
    }
}
