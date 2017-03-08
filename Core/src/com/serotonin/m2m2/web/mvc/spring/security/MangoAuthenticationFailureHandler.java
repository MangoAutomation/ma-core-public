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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.module.DefaultPagesDefinition;

/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    RequestMatcher browserHtmlRequestMatcher;

    @Autowired
    public MangoAuthenticationFailureHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher) {
        this.setAllowSessionCreation(false);
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
    }
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        
        if (browserHtmlRequestMatcher.matches(request)) {
            saveException(request, exception);
            
            String uri = DefaultPagesDefinition.getLoginUri(request, response);
            
            String errorKey;
            if (exception instanceof DisabledException) {
                errorKey = "login.validation.accountDisabled";
            } else {
                errorKey = "login.validation.invalidLogin";
            }
            
            uri = UriComponentsBuilder.fromUriString(uri)
                .queryParam("error", errorKey)
                .queryParam("username", request.getParameter("username"))
                .build()
                .toUriString();

            if (this.isUseForward()) {
                logger.debug("Forwarding to " + uri);
                request.getRequestDispatcher(uri).forward(request, response);
            } else {
                logger.debug("Redirecting to " + uri);
                this.getRedirectStrategy().sendRedirect(request, response, uri);
            }
        } else {
            request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
            // forward the request on to its usual destination (e.g. /rest/v1/login) so the correct response is returned
            request.getRequestDispatcher(request.getRequestURI()).forward(request, response);
        }
    }
}
