/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class MangoSwitchUserFilter extends SwitchUserFilter {
    RequestMatcher suMatcher = new AntPathRequestMatcher("/rest/*/login/su", HttpMethod.POST.name());
    RequestMatcher exitSuMatcher = new AntPathRequestMatcher("/rest/*/login/exit-su", HttpMethod.POST.name());

    @Override
    protected boolean requiresSwitchUser(HttpServletRequest request) {
        return suMatcher.matches(request);
    }

    @Override
    protected boolean requiresExitUser(HttpServletRequest request) {
        return exitSuMatcher.matches(request);
    }
}