/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.IpAddressAuthenticationRateException;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.UsernameAuthenticationRateException;
import com.infiniteautomation.mango.spring.components.RunAs;

/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Log log = LogFactory.getLog(MangoAuthenticationFailureHandler.class);

    private final RequestMatcher browserHtmlRequestMatcher;
    private final RunAs runAs;

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
    public MangoAuthenticationFailureHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher, RunAs runAs) {
        this.runAs = runAs;
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

        if (log.isWarnEnabled()) {
            this.logException(request, exception);
        }

        saveExceptionImpl(request, exception);

        if (browserHtmlRequestMatcher.matches(request)) {
            if (exception instanceof CredentialsExpiredException) {
                String uri = DefaultPagesDefinition.getPasswordResetUri();
                uri = UriComponentsBuilder.fromUriString(uri).build().toUriString();
                this.getRedirectStrategy().sendRedirect(request, response, uri);
                return;
            }

            String uri;

            String referrer = request.getHeader(HttpHeaders.REFERER);
            String referrerPath = null;

            if (referrer != null) {
                referrerPath = UriComponentsBuilder.fromUriString(referrer).build().getPath();
            }

            // redirect back to the referrer for browser requests, stops logins at /login.htm being directed back to /ui/login everytime
            if (referrerPath != null) {
                uri = referrerPath;
            } else {
                uri = DefaultPagesDefinition.getLoginUri(request, response);
            }

            uri = UriComponentsBuilder.fromUriString(uri)
                    .build()
                    .toUriString();

            log.debug("Redirecting to " + uri);
            this.getRedirectStrategy().sendRedirect(request, response, uri);
        } else {
            // forward the request on to its usual destination (e.g. /rest/v1/login) so the correct response is returned
            request.getRequestDispatcher(request.getRequestURI()).forward(request, response);
        }
    }

    private String getUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute(JsonUsernamePasswordAuthenticationFilter.USERNAME_ATTRIBUTE);
        if (username == null) {
            username = request.getParameter("username");
        }
        return username;
    }

    protected void saveExceptionImpl(HttpServletRequest request, AuthenticationException exception) {
        String username = this.getUsername(request);
        String ipAddress = request.getRemoteAddr();

        request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
            //Store for use in the Controller
            session.setAttribute("username", username);
        }

        //Raise the event
        runAs.runAs(PermissionHolder.SYSTEM_SUPERADMIN, () -> {
            // need permission to access com.infiniteautomation.mango.spring.service.MailingListService.getAlarmAddresses
            SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_FAILED_USER_LOGIN), Common.timer.currentTimeMillis(), false, new TranslatableMessage("event.failedLogin", username, ipAddress));
        });
    }

    private void logException(HttpServletRequest request, AuthenticationException exception) {
        String username = this.getUsername(request);
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
        } else {
            log.warn("Failed login attempt on user '" + username + "' from IP " + ipAddress);
        }
    }
}
