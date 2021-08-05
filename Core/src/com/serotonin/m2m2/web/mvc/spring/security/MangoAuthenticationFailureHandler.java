/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.IpAddressAuthenticationRateException;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.UsernameAuthenticationRateException;

/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(MangoAuthenticationFailureHandler.class);

    private final RequestMatcher browserHtmlRequestMatcher;
    private final RunAs runAs;
    private final PageResolver pageResolver;

    /**
     * Stores a boolean to indicate if an attack by an IP was already logged, stops the logs being flooded.
     * Will reset after 5 minutes of no attacks being detected.
     */
    private final Cache<String, Boolean> messageAlreadyLogged;

    /**
     * Stores a boolean to indicate if an attack against a username was already logged, stops the logs being flooded.
     * Will reset after 5 minutes of no attacks being detected.
     */
    private final Cache<String, Boolean> rateLimitUsernameLogged;

    @Autowired
    public MangoAuthenticationFailureHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher, RunAs runAs, PageResolver pageResolver) {
        this.runAs = runAs;
        this.pageResolver = pageResolver;
        this.setAllowSessionCreation(false);
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;

        if (log.isWarnEnabled()) {
            this.messageAlreadyLogged = Caffeine.newBuilder()
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();
            this.rateLimitUsernameLogged = Caffeine.newBuilder()
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();
        } else {
            this.messageAlreadyLogged = null;
            this.rateLimitUsernameLogged = null;
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

        String username = this.getUsername(request);
        if (log.isWarnEnabled()) {
            this.logException(request, exception, username);
        }

        saveExceptionImpl(request, exception, username);

        if (browserHtmlRequestMatcher.matches(request)) {
            String baseUri;
            if (exception instanceof CredentialsExpiredException) {
                baseUri = pageResolver.getPasswordResetUri();
            } else {
                baseUri = pageResolver.getLoginErrorUri(request, response);
            }

            String uri = UriComponentsBuilder.fromUriString(baseUri)
                    .queryParamIfPresent("username", Optional.ofNullable(username))
                    .queryParam("error", exception.getMessage())
                    .toUriString();

            if (log.isDebugEnabled()) {
                log.debug("Redirecting to " + uri);
            }
            getRedirectStrategy().sendRedirect(request, response, uri);
        } else {
            // forward the request on to its usual destination (e.g. /rest/v1/login) so the correct response is returned
            request.getRequestDispatcher(request.getRequestURI()).forward(request, response);
        }
    }

    private String getUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute(JsonUsernamePasswordAuthenticationFilter.USERNAME_ATTRIBUTE);
        if (username == null) {
            // form based login
            username = request.getParameter("username");
        }
        return username;
    }

    protected void saveExceptionImpl(HttpServletRequest request, AuthenticationException exception, String username) {
        request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);

        if (exception instanceof BadCredentialsException || exception instanceof DisabledException || exception instanceof LockedException) {
            String ipAddress = request.getRemoteAddr();
            //Raise the event
            runAs.runAs(runAs.systemSuperadmin(), () -> {
                // need permission to access com.infiniteautomation.mango.spring.service.MailingListService.getAlarmAddresses
                SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_FAILED_USER_LOGIN), Common.timer.currentTimeMillis(), false, new TranslatableMessage("event.failedLogin", username, ipAddress));
            });
        }
    }

    private void logException(HttpServletRequest request, AuthenticationException exception, String username) {
        String ipAddress = request.getRemoteAddr();

        if (exception instanceof IpAddressAuthenticationRateException) {
            if (messageAlreadyLogged != null && messageAlreadyLogged.getIfPresent(ipAddress) != Boolean.TRUE) {
                messageAlreadyLogged.put(ipAddress, Boolean.TRUE);
                log.warn("Possible brute force attack, authentication attempt rate limit exceeded for IP address " + ipAddress);
            }
        } else if (exception instanceof UsernameAuthenticationRateException) {
            if (rateLimitUsernameLogged != null && rateLimitUsernameLogged.getIfPresent(username) != Boolean.TRUE) {
                rateLimitUsernameLogged.put(username, Boolean.TRUE);
                log.warn("Possible brute force attack, authentication attempt rate limit exceeded against username " + username);
            }
        } else if (exception instanceof BadCredentialsException || exception instanceof DisabledException || exception instanceof LockedException) {
            log.warn("Failed login attempt on user '" + username + "' from IP " + ipAddress);
        } else {
            log.warn("Error while authenticating IP " + ipAddress, exception);
        }
    }
}
