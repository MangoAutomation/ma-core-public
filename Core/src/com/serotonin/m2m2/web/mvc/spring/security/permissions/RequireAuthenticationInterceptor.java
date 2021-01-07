/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.permissions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
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
