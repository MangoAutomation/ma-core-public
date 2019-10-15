/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.servlets.DoSFilter;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.serotonin.m2m2.vo.User;

/**
 * https://www.eclipse.org/jetty/documentation/current/dos-filter.html
 *
 * @author Jared Wiltshire
 */
@Component(MangoDosFilter.NAME)
@ConditionalOnProperty("${web.dos.enabled:true}")
@WebFilter(
        filterName = MangoDosFilter.NAME,
        asyncSupported = true,
        urlPatterns = {"/*"},
        initParams = {
                @WebInitParam(name = "managedAttr", value = "false"),
                @WebInitParam(name = "maxRequestsPerSec", value = "${web.dos.maxRequestsPerSec:25}"),
                @WebInitParam(name = "delayMs", value = "${web.dos.delayMs:100}"),
                @WebInitParam(name = "throttledRequests", value = "${web.dos.throttledRequests:5}"),
                @WebInitParam(name = "maxWaitMs", value = "${web.dos.maxWaitMs:50}"),
                @WebInitParam(name = "throttleMs", value = "${web.dos.throttleMs:30000}"),
                @WebInitParam(name = "maxRequestMs", value = "${web.dos.maxRequestMs:30000}"),
                @WebInitParam(name = "maxIdleTrackerMs", value = "${web.dos.maxIdleTrackerMs:30000}"),
                @WebInitParam(name = "insertHeaders", value = "${web.dos.insertHeaders:true}"),
                @WebInitParam(name = "trackSessions", value = "${web.dos.trackSessions:true}"),
                @WebInitParam(name = "remotePort", value = "${web.dos.remotePort:false}"),
                @WebInitParam(name = "ipWhitelist", value = "${web.dos.ipWhitelist:}")
        },
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.ASYNC})
@Order(FilterOrder.DOS)
public class MangoDosFilter extends DoSFilter {
    public static final String NAME = "mangoDosFilter";

    /**
     * To enable giving priority to logged in users
     */
    @Override
    protected String extractUserId(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Object context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (context instanceof SecurityContext) {
                SecurityContext securityContext = (SecurityContext) context;
                Authentication auth = securityContext.getAuthentication();
                if (auth != null) {
                    Object principle = auth.getPrincipal();
                    if (principle instanceof User) {
                        User user = (User) principle;
                        return Integer.toString(user.getId());
                    }
                    return auth.getName();
                }
            }
        }

        return null;
    }
}