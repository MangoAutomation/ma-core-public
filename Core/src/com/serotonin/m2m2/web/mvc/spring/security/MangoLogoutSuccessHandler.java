/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;

/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {
    private final RequestMatcher browserHtmlRequestMatcher;
    private final RequestMatcher restLogoutRequestMatcher;
    private final PageResolver pageResolver;
    
    @Autowired
    public MangoLogoutSuccessHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher, PageResolver pageResolver) {
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
        this.pageResolver = pageResolver;
        this.restLogoutRequestMatcher = new AntPathRequestMatcher("/rest/*/logout", "POST");
    }
    
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (browserHtmlRequestMatcher.matches(request)) {
            super.onLogoutSuccess(request, response, authentication);
        } else if (restLogoutRequestMatcher.matches(request)) {
            // forward the request on to its usual destination (e.g. /rest/v1/logout) so the correct response is returned
            request.getRequestDispatcher(request.getRequestURI()).forward(request, response);
        } else {
            response.setStatus(HttpStatus.OK.value());
            response.getWriter().flush();
        }
    }
    
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String url = super.determineTargetUrl(request, response);
        if (url == null || url.equals(this.getDefaultTargetUrl())) {
            return pageResolver.getLoginUri(request, response);
        }
        return url;
    }
}
