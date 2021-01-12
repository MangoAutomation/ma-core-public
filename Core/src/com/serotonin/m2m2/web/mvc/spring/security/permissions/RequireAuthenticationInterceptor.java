/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.permissions;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class RequireAuthenticationInterceptor extends HandlerInterceptorAdapter {

    final AuthenticationTrustResolver trustResolver;

    @Autowired
    public RequireAuthenticationInterceptor(AuthenticationTrustResolver trustResolver) {
        this.trustResolver = trustResolver;
    }

    /**
     * <p>We don't handle ASYNC dispatches as the {@link Authentication} is not populated correctly when using token/basic authentication.
     * The authentication should already have been checked during the original REQUEST/FORWARD dispatch.</p>
     *
     * <p>The authentication is anonymous during ASYNC dispatch for token/basic auth as they use a stateless security configuration and do not use sessions.
     * This configuration does not have a {@link SecurityContextRepository} and therefore has no way to persist the security context between the
     * REQUEST and ASYNC dispatch (usually performed by {@link SecurityContextPersistenceFilter}).</p>
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getDispatcherType() != DispatcherType.ASYNC && handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            if (!method.hasMethodAnnotation(AnonymousAccess.class)) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || trustResolver.isAnonymous(auth)) {
                    throw new AccessDeniedException("Anonymous access is not allowed");
                }
            }
        }
        return true;
    }
}
