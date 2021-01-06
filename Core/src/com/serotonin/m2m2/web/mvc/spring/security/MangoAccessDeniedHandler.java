/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * <p>This handles requests where the user is authenticated. See also {@link MangoAuthenticationEntryPoint} which handles requests where the user is not authenticated.</p>
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
@Component
@Primary
public class MangoAccessDeniedHandler implements AccessDeniedHandler {
    final RequestMatcher browserHtmlRequestMatcher;
    final Environment env;

    @Autowired
    public MangoAccessDeniedHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher, Environment env) {
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
        this.env = env;
    }

    private final Log log = LogFactory.getLog(MangoAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        log.warn("Denying access to Mango resource " + request.getRequestURI() + " to IP " + request.getRemoteAddr());

        if (!response.isCommitted()) {
            if (!env.getProperty("rest.disableErrorRedirects", Boolean.class, false) && browserHtmlRequestMatcher.matches(request)) {
                // browser HTML request
                String accessDeniedUrl = null;

                PermissionHolder user = Common.getUser();
                if (user instanceof User) {
                    log.warn("Denied user is " + user.getPermissionHolderName());
                    accessDeniedUrl = DefaultPagesDefinition.getUnauthorizedUri(request, response, (User) user);
                } else {
                    log.warn("Denied permission holder is " + user.getPermissionHolderName());
                }

                if (accessDeniedUrl != null) {
                    // redirect to error page.
                    response.sendRedirect(accessDeniedUrl);
                    return;
                }
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
        }
    }
}