/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.springframework.web.servlet.DispatcherServlet;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.web.mvc.spring.security.BrowserRequestMatcher;

/**
 * Handles calls to {@link HttpServletResponse#sendError(int)} and exceptions that are thrown outside of the Spring {@link DispatcherServlet}
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class MangoErrorHandler extends ErrorHandler {

    @Override
    public void doError(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (!Common.envProps.getBoolean("rest.disableErrorRedirects", false) && BrowserRequestMatcher.INSTANCE.matches(request)) {
                String uri;
                if (response.getStatus() == 404) {
                    uri = DefaultPagesDefinition.getNotFoundUri(request, response);
                } else {
                    uri = DefaultPagesDefinition.getErrorUri(baseRequest, response);
                }
                if (uri != null) {
                    response.sendRedirect(uri);
                }
            }
        } finally {
            baseRequest.setHandled(true);
        }
    }
}
