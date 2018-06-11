/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * @author Jared Wiltshire
 *
 */
@Component("restAccessDeniedHandler")
public class MangoRestAccessDeniedHandler implements AccessDeniedHandler {

    private final Log LOG = LogFactory.getLog(MangoRestAccessDeniedHandler.class);
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        LOG.warn("Denying access to Mango rest resource " + request.getRequestURI() + " to IP " + request.getRemoteAddr(), accessDeniedException);
        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
        }
    }

}
