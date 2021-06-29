/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;

import com.serotonin.m2m2.web.mvc.spring.security.BrowserRequestMatcher;

/**
 * @author Jared Wiltshire
 */
public class StartupServlet extends DefaultServlet {

    private static final long serialVersionUID = -1349303330406758755L;
    private static final String STARTUP_URL = "/startup/index.html";

    public StartupServlet() {
        super(new StartupResourceService());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    private static class StartupResourceService extends ResourceService {
        @Override
        protected void notFound(HttpServletRequest request, HttpServletResponse response) throws IOException {
            if (BrowserRequestMatcher.INSTANCE.matches(request)) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

                try {
                    request.getRequestDispatcher(STARTUP_URL).forward(request, response);
                } catch (ServletException e) {
                    throw new RuntimeException(e);
                }
            } else {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        }

        @Override
        protected void sendDirectory(HttpServletRequest request, HttpServletResponse response, Resource resource, String pathInContext) throws IOException {
            if (BrowserRequestMatcher.INSTANCE.matches(request)) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

                try {
                    request.getRequestDispatcher(STARTUP_URL).forward(request, response);
                } catch (ServletException e) {
                    throw new RuntimeException(e);
                }
            } else {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        }
    }
}
