/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Modified version of org.springframework.security.web.csrf.CsrfFilter.DefaultRequiresCsrfMatcher
 *
 * Requires CSRF protection for PUT and POST requests except when token authenticated or a whitelisted url
 *
 * @author Jared Wiltshire
 */
@Component
public class MangoRequiresCsrfMatcher implements RequestMatcher {
    private final HashSet<String> allowedMethods = new HashSet<String>(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"));

    private final RequestMatcher isAllowedUrl = new OrRequestMatcher(
            new AntPathRequestMatcher("/httpds"),
            new AntPathRequestMatcher("/dwr/**"),
            new AntPathRequestMatcher("/cloud-connect-proxy/dwr/**"),
            new AntPathRequestMatcher("/haystack/**"));

    @Override
    public boolean matches(HttpServletRequest request) {
        if (isAllowedUrl.matches(request)) {
            return false;
        }
        return !this.allowedMethods.contains(request.getMethod());
    }
}
