/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class MangoSwitchUserFilter extends SwitchUserFilter {

    RequestMatcher suMatcher = new AntPathRequestMatcher("/rest/*/login/su", HttpMethod.POST.name());
    RequestMatcher exitSuMatcher = new AntPathRequestMatcher("/rest/*/login/exit-su", HttpMethod.POST.name());

    @Autowired
    public MangoSwitchUserFilter(UserDetailsService userDetailsService, AuthenticationSuccessHandler authenticationSuccessHandler,
            AuthenticationFailureHandler failureHandler, ApplicationEventPublisher eventPublisher) {
        this.setUserDetailsService(userDetailsService);
        this.setSuccessHandler(authenticationSuccessHandler);
        this.setFailureHandler(failureHandler);
        this.setApplicationEventPublisher(eventPublisher);
        this.setUsernameParameter("username");
        this.setAuthenticationDetailsSource(new MangoAuthenticationDetailsSource(false));
    }

    @Override
    protected boolean requiresSwitchUser(HttpServletRequest request) {
        return suMatcher.matches(request);
    }

    @Override
    protected boolean requiresExitUser(HttpServletRequest request) {
        return exitSuMatcher.matches(request);
    }

}