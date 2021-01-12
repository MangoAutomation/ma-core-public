/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import com.serotonin.m2m2.web.mvc.spring.security.permissions.RequireAuthenticationInterceptor;

/**
 * <p>Stores the security context from the REQUEST/FORWARD dispatch in a request attribute and retrieves it for ASYNC dispatch.
 * We added this class after discovering that {@link RequireAuthenticationInterceptor} doesn't work for ASYNC dispatches</p>
 * <p>To ensure this solution is correct, we created <a href="https://github.com/spring-projects/spring-security/issues/9342">an issue</a> on the Spring Security repo.</p>
 *
 * @author Jared Wiltshire
 */
class StatelessSecurityContextRepository implements SecurityContextRepository {
    private final String SECURITY_CONTEXT_ATTRIBUTE = "MANGO_SECURITY_CONTEXT";

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        HttpServletRequest request = requestResponseHolder.getRequest();
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            Object securityContext = request.getAttribute(SECURITY_CONTEXT_ATTRIBUTE);
            if (securityContext instanceof SecurityContext) {
                return (SecurityContext) securityContext;
            }
        }
        return SecurityContextHolder.createEmptyContext();
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        if (request.getDispatcherType() != DispatcherType.ASYNC) {
            request.setAttribute(SECURITY_CONTEXT_ATTRIBUTE, context);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return request.getAttribute(SECURITY_CONTEXT_ATTRIBUTE) instanceof SecurityContext;
    }
}
