/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * @author Matthew Lohbihler
 */
public class CharacterSetFilter implements Filter {
    private String charset;

    public void init(FilterConfig filterConfig) {
        charset = filterConfig.getInitParameter("charset");
    }

    public void destroy() {
        // no op
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (servletRequest.getCharacterEncoding() == null)
            servletRequest.setCharacterEncoding(charset);
        servletResponse.setCharacterEncoding(charset);

        // Continue with the chain.
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
