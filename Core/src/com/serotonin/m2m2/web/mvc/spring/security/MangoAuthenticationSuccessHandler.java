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

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.module.AuthenticationDefinition;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    final RequestMatcher browserHtmlRequestMatcher;
    RequestCache requestCache;
    
    @Autowired
    public MangoAuthenticationSuccessHandler(RequestCache requestCache,
            @Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher) {
        this.setRequestCache(requestCache);
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
    }
    
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String url = super.determineTargetUrl(request, response);
        if (url == null || url.equals(this.getDefaultTargetUrl())) {
            User user = Common.getHttpUser();
            return DefaultPagesDefinition.getDefaultUri(request, response, user);
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
        User user = Common.getHttpUser();
        
        HttpSession session = request.getSession(false);
        if (session != null && user != null) {
            // For legacy pages also update the session
            session.setAttribute(Common.SESSION_USER, user);
            
            // Update the last login time.
            DaoRegistry.userDao.recordLogin(user.getId());
            
            // Set the IP Address for the session
            user.setRemoteAddr(request.getRemoteAddr());
            
            Common.loginManager.getSessionCountMonitor().increment();
        }

        if (user != null) {
            for (AuthenticationDefinition def : ModuleRegistry.getDefinitions(AuthenticationDefinition.class)) {
                def.postLogin(user);
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
