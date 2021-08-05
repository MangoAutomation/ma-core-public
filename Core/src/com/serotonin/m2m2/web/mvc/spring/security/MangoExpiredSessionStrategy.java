/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
@Component
public class MangoExpiredSessionStrategy implements SessionInformationExpiredStrategy {

    private final RequestMatcher browserHtmlRequestMatcher;
    private final Logger log = LoggerFactory.getLogger(MangoExpiredSessionStrategy.class);
    private final PageResolver pageResolver;

    @Autowired
    public MangoExpiredSessionStrategy(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher, PageResolver pageResolver) {
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
        this.pageResolver = pageResolver;
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
            String loginUri = pageResolver.getLoginUri(request, response);
            response.sendRedirect(loginUri);
        } else {
            TranslatableMessage message = new TranslatableMessage("rest.exception.sessionExpired");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message.translate(Common.getTranslations()));
        }
    }
}
