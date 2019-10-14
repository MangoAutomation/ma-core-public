/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import java.security.Principal;

import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.servlets.QoSFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * https://www.eclipse.org/jetty/documentation/current/qos-filter.html
 * @author Jared Wiltshire
 */
@Component(MangoQosFilter.NAME)
@ConditionalOnProperty("${web.dos.enabled:true}")
@WebFilter(
        filterName = MangoQosFilter.NAME,
        asyncSupported = true,
        urlPatterns = {"/*"},
        initParams = {
                @WebInitParam(name = "managedAttr", value = "false"),
                @WebInitParam(name = "maxRequests", value = "${web.qos.maxRequests:10}"),
                @WebInitParam(name = "maxPriority", value = "3"),
                @WebInitParam(name = "waitMs", value = "${web.qos.waitMs:50}"),
                @WebInitParam(name = "suspendMs", value = "${web.qos.suspendMs:-1}")
        },
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.ASYNC})
@Order(FilterOrder.QOS)
public class MangoQosFilter extends QoSFilter {
    public static final String NAME = "mangoQosFilter";

    /**
     * Computes the request priority.
     * <p>
     * The default implementation assigns the following priorities:
     * <ul>
     * <li> 3 - for an authenticated superadmin request
     * <li> 2 - for an authenticated request
     * <li> 1 - for a request with valid / non new session
     * <li> 0 - for all other requests.
     * </ul>
     * This method may be overridden to provide application specific priorities.
     *
     * @param request the incoming request
     * @return the computed request priority
     */
    @Override
    protected int getPriority(ServletRequest request) {
        // Override default priority levels to ensure superadmin gets the highest priority
        HttpServletRequest baseRequest = (HttpServletRequest)request;
        Principal principle = baseRequest.getUserPrincipal();
        if (principle != null) {
            //are we a mango user
            User user = Common.getHttpUser();
            if(user != null) {
                return Permissions.hasAdminPermission(user) ? 3 : 2;
            }else {
                return 2;
            }
        } else {
            HttpSession session = baseRequest.getSession(false);
            if (session != null && !session.isNew()) {
                return 1;
            }else {
                return 0;
            }
        }
    }
}