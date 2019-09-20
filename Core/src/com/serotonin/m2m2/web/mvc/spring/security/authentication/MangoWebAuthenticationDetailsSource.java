/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.web.mvc.spring.security.RateLimitingFilter;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoWebAuthenticationDetailsSource.MangoWebAuthenticationDetails;

/**
 * @author Jared Wiltshire
 */
@Component
public class MangoWebAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, MangoWebAuthenticationDetails> {

    private final boolean honorXForwardedFor;

    private MangoWebAuthenticationDetailsSource(@Value("${rateLimit.honorXForwardedFor:false}") boolean honorXForwardedFor) {
        this.honorXForwardedFor = honorXForwardedFor;
    }

    @Override
    public MangoWebAuthenticationDetails buildDetails(HttpServletRequest request) {
        return new MangoWebAuthenticationDetails(request, honorXForwardedFor);
    }


    public static class MangoWebAuthenticationDetails extends WebAuthenticationDetails {
        private static final long serialVersionUID = -8339577766938807428L;

        private final boolean sessionIsNew;
        private final String remoteAddress;

        public MangoWebAuthenticationDetails(HttpServletRequest request, boolean honorXForwardedFor) {
            super(request);

            this.remoteAddress = RateLimitingFilter.getRemoteAddr(request, honorXForwardedFor);
            HttpSession session = request.getSession(false);
            if (session == null) {
                sessionIsNew = false;
            } else {
                sessionIsNew = session.isNew();
            }
        }

        @Override
        public String getRemoteAddress() {
            return this.remoteAddress;
        }

        public boolean isSessionIsNew() {
            return sessionIsNew;
        }
    }
}
