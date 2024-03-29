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
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * <p>This handles requests where the user is authenticated but does not have access, and also when there is no
 * authentication entry point such as when using basic or token authentication.</p>
 * <p>See also {@link MangoAuthenticationEntryPoint} which handles requests where the user is not authenticated.</p>
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
@Component
@Primary
public class MangoAccessDeniedHandler implements AccessDeniedHandler {
    final RequestMatcher browserHtmlRequestMatcher;
    final Environment env;
    final PageResolver pageResolver;

    @Autowired
    public MangoAccessDeniedHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher, Environment env, PageResolver pageResolver) {
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
        this.env = env;
        this.pageResolver = pageResolver;
    }

    private final Logger log = LoggerFactory.getLogger(MangoAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        PermissionHolder user;
        try {
            user = Common.getUser();
        } catch (PermissionException e) {
            // this can occur with basic/token auth as an authentication entry point is not used for those auth types
            user = null;
        }

        if (log.isWarnEnabled()) {
            log.warn(String.format("Access denied to resource '%s', for user '%s', IP address '%s'", request.getRequestURI(),
                    user == null ? "" :user.getPermissionHolderName(), request.getRemoteAddr()));
        }

        if (!env.getProperty("rest.disableErrorRedirects", Boolean.class, false) && browserHtmlRequestMatcher.matches(request)) {
            String accessDeniedUrl = pageResolver.getUnauthorizedUri(request, response, user == null ? null : user.getUser());
            if (accessDeniedUrl != null) {
                // redirect to error page.
                response.sendRedirect(accessDeniedUrl);
                return;
            }
        }

        response.sendError(user == null ? HttpServletResponse.SC_UNAUTHORIZED: HttpServletResponse.SC_FORBIDDEN,
                accessDeniedException.getMessage());
    }
}