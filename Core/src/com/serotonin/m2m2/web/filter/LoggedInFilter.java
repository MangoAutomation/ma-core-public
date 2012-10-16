/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.util.license.LicenseFeature;
import com.serotonin.m2m2.vo.User;

public class LoggedInFilter implements Filter {
    private final Log LOGGER = LogFactory.getLog(LoggedInFilter.class);

    private String forwardUrl;

    // Free mode checking should arguably be done in a separate filter, but it becomes too easy to just comment out 
    // such a filter in the web.xml file, so we do it here in a place where it is more secure.
    private int maxUniqueIps;
    private final List<String> usedIpAddresses = new ArrayList<String>();
    private String exceededIpLimitUrl;

    @Override
    public void init(FilterConfig config) {
        forwardUrl = config.getInitParameter("forwardUrl");
        exceededIpLimitUrl = config.getInitParameter("exceededIpLimitUrl");

        LicenseFeature uniqueIpAddresses = Common.licenseFeature("uniqueIpAddresses");
        if (uniqueIpAddresses == null)
            maxUniqueIps = 3;
        else {
            if ("unlimited".equals(uniqueIpAddresses.getValue()))
                maxUniqueIps = -1;
            else
                maxUniqueIps = Integer.parseInt(uniqueIpAddresses.getValue());
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // Assume an http request.
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (maxUniqueIps != -1) {
            // Check the list of IP addresses. If this is a new IP, and there are no more available slots, deny the 
            // request.
            String ip = request.getRemoteAddr();
            if (!usedIpAddresses.contains(ip)) {
                // This is a new IP address. Check if the limit is exceeded.
                if (usedIpAddresses.size() >= maxUniqueIps) {
                    // Deny the request.
                    LOGGER.info("Denying access to request from IP " + ip + ". Used IP addresses: " + usedIpAddresses);
                    response.sendRedirect(exceededIpLimitUrl);
                    return;
                }

                // Otherwise we add the address and continue.
                usedIpAddresses.add(ip);
            }
        }

        boolean loggedIn = true;
        User user = Common.getUser(request);
        if (user == null)
            loggedIn = false;
        else {
            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
                loggedIn = def.isAuthenticated(request, response, user);
                if (!loggedIn)
                    break;
            }
        }

        if (!loggedIn) {
            LOGGER.info("Denying access to secure page for session id " + request.getSession().getId() + ", uri="
                    + request.getRequestURI());
            response.sendRedirect(forwardUrl);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        // no op
    }
}
