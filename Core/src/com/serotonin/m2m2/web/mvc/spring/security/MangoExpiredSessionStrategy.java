/*
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DefaultPagesDefinition;

/**
 * @author Jared Wiltshire
 */
public class MangoExpiredSessionStrategy implements SessionInformationExpiredStrategy {

    private RequestMatcher browserHtmlRequestMatcher;
    private final Log log = LogFactory.getLog(MangoExpiredSessionStrategy.class);

    @Autowired
    public MangoExpiredSessionStrategy(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher) {
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
    }
    
    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException, ServletException {
        HttpServletRequest request = event.getRequest();
        HttpServletResponse response = event.getResponse();
        
        if (log.isDebugEnabled()) {
            log.debug(String.format("Expired session detected, request URI is %s", request.getRequestURI()));
        }
        
        if (response.isCommitted()) return;

        if (this.browserHtmlRequestMatcher.matches(request)) {
            String loginUri = DefaultPagesDefinition.getLoginUri(request, response);
            response.sendRedirect(loginUri);
        } else {
            TranslatableMessage message = new TranslatableMessage("rest.exception.sessionExpired");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message.translate(Common.getTranslations()));
        }
    }
}
