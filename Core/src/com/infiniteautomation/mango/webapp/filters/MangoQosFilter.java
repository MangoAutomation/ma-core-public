/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.servlets.QoSFilter;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * https://www.eclipse.org/jetty/documentation/current/qos-filter.html
 * @author Jared Wiltshire
 */
@Component(MangoQosFilter.NAME)
@ConditionalOnProperty("${web.qos.enabled:false}")
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

    private final PermissionService permissionService;
    
    public MangoQosFilter(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
    
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
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Object context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (context instanceof SecurityContext) {
                SecurityContext securityContext = (SecurityContext) context;
                Authentication auth = securityContext.getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof PermissionHolder) {
                    PermissionHolder user = (PermissionHolder) auth.getPrincipal();
                    return permissionService.hasAdminRole(user) ? 3 : 2;
                }
            }

            if (!session.isNew()) {
                return 1;
            }
        }

        return 0;
    }
}