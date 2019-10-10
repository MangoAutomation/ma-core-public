/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.eclipse.jetty.servlets.DoSFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

/**
 * https://www.eclipse.org/jetty/documentation/current/dos-filter.html
 *
 * @author Jared Wiltshire
 */
@Component(MangoDosFilter.NAME)
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
        })
@Order(100)
public class MangoDosFilter extends DoSFilter {
    public static final String NAME = "mangoDosFilter";

    /**
     * To enable giving priority to logged in users
     */
    @Override
    protected String extractUserId(ServletRequest request) {
        User user = Common.getHttpUser();
        if (user != null) {
            return Integer.toString(user.getId());
        } else {
            return null;
        }
    }
}