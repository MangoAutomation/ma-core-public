/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;

/**
 * <p>This handles requests where the user is not authenticated. See also {@link MangoAccessDeniedHandler} which handles requests where the user is authenticated but does not have access.</p>
 *
 * @author Jared Wiltshire
 */
@Component
public class MangoAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    final RequestMatcher browserHtmlRequestMatcher;
    final Environment env;
    final PageResolver pageResolver;

    @Autowired
    public MangoAuthenticationEntryPoint(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher, Environment env, PageResolver pageResolver) {
        // this URL is not actually used
        super("/login.htm");

        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
        this.env = env;
        this.pageResolver = pageResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        if (!env.getProperty("rest.disableErrorRedirects", Boolean.class, false) && browserHtmlRequestMatcher.matches(request)) {
            super.commence(request, response, authException);
        } else {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), authException.getMessage());
        }
    }

    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        return pageResolver.getLoginUri(request, response);
    }
}
