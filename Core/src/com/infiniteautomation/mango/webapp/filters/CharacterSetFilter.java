/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.infiniteautomation.mango.webapp.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.springframework.stereotype.Component;

/**
 * @author Matthew Lohbihler
 */
@Component
@WebFilter(
        urlPatterns = {"*.htm", "*.shtm"},
        initParams = {
                @WebInitParam(name = "charset", value = "UTF-8")
        })
public class CharacterSetFilter implements Filter {
    private String charset;

    @Override
    public void init(FilterConfig filterConfig) {
        charset = filterConfig.getInitParameter("charset");
    }

    @Override
    public void destroy() {
        // no op
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (servletRequest.getCharacterEncoding() == null)
            servletRequest.setCharacterEncoding(charset);
        servletResponse.setCharacterEncoding(charset);

        // Continue with the chain.
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
