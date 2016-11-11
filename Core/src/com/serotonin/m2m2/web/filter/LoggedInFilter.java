/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;

public class LoggedInFilter implements Filter {
    private final Log LOGGER = LogFactory.getLog(LoggedInFilter.class);

    private String exceededIpLimitUrl;

    @Override
    public void init(FilterConfig config) {
        exceededIpLimitUrl = config.getInitParameter("exceededIpLimitUrl");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // Assume an http request.
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        
        
        //Check to see if we are valid to access Mango
        if(!Common.loginManager.isValidIp(request)){
        	response.sendRedirect(exceededIpLimitUrl);
        }

        //Did we already come from a secure URL?
        if(Common.loginManager.isSecure(request)){
        	filterChain.doFilter(servletRequest, servletResponse);
        	return;
        }

        //Are we logged in?
        if (!Common.loginManager.isLoggedIn(request, response)) {
            LOGGER.warn("Denying access to secure page for session id " + request.getSession().getId() + ", uri="
                    + request.getRequestURI());

            String forwardUri = DefaultPagesDefinition.getLoginUri(request, response);
            request.getSession().invalidate();
            response.sendRedirect(forwardUri);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        // no op
    }
}
