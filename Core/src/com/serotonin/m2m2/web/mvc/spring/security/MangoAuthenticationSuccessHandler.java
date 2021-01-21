/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.pageresolver.LoginUriInfo;
import com.infiniteautomation.mango.spring.components.pageresolver.PageResolver;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    final RequestMatcher browserHtmlRequestMatcher;
    RequestCache requestCache;
    final UserDao userDao;
    final PageResolver pageResolver;

    @Autowired
    public MangoAuthenticationSuccessHandler(
            RequestCache requestCache,
            @Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher,
            UserDao userDao, PageResolver pageResolver) {
        this.pageResolver = pageResolver;
        this.setRequestCache(requestCache);
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
        this.userDao = userDao;
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String url = super.determineTargetUrl(request, response);
        if (url == null || url.equals(this.getDefaultTargetUrl())) {
            LoginUriInfo info = pageResolver.getDefaultUriInfo(request, response, Common.getUser().getUser());
            return info.getUri();
        }
        return url;
    }

    @Override
    public void setRequestCache(RequestCache requestCache) {
        super.setRequestCache(requestCache);
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        PermissionHolder permissionHolder = principal instanceof PermissionHolder ? (PermissionHolder) principal : null;
        User user = permissionHolder != null ? permissionHolder.getUser() : null;

        //Set the per-user session timeout
        if(user != null && user.isSessionExpirationOverride()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setMaxInactiveInterval((int)(Common.getMillis(Common.TIME_PERIOD_CODES.getId(user.getSessionExpirationPeriodType()), user.getSessionExpirationPeriods())/1000L));
            }
        }

        if (browserHtmlRequestMatcher.matches(request)) {
            super.onAuthenticationSuccess(request, response, authentication);
        } else {
            requestCache.removeRequest(request, response);
            clearAuthenticationAttributes(request);

            // forward the request on to its usual destination (e.g. /rest/v1/login) so the correct response is returned
            request.getRequestDispatcher(request.getRequestURI()).forward(request, response);
        }
    }
}
