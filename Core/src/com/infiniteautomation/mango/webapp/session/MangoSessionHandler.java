/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.webapp.session;

import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionListener;

/**
 * @author Jared Wiltshire
 */
@Component
public class MangoSessionHandler extends SessionHandler {

    private final Environment env;
    private final MangoSessionListener sessionListener;
    private final MangoSessionCookieConfig cookieConfig;

    @Autowired
    public MangoSessionHandler(SessionDataStore sessionDataStore, Environment env, MangoSessionListener sessionListener) {
        this.env = env;
        this.sessionListener = sessionListener;
        this.cookieConfig = new MangoSessionCookieConfig();

        SessionCache sessionCache = new DefaultSessionCache(this);
        sessionCache.setSessionDataStore(sessionDataStore);
        setSessionCache(sessionCache);

        // Disable the JSESSIONID URL Parameter
        setSessionIdPathParameterName("none");
    }

    @Override
    public HttpCookie getSessionCookie(HttpSession session, String contextPath, boolean requestIsSecure) {
        if (isUsingCookies()) {
            String sessionPath = cookieConfig.getPath();
            if (sessionPath == null) {
                sessionPath = StringUtil.isEmpty(contextPath) ? "/" : contextPath;
            }
            String comment = cookieConfig.getComment();

            return new HttpCookie(
                    cookieConfig.getName(),
                    getExtendedId(session),
                    cookieConfig.getDomain(),
                    sessionPath,
                    cookieConfig.getMaxAge(),
                    cookieConfig.isHttpOnly(),
                    cookieConfig.isSecure() || (isSecureRequestOnly() && requestIsSecure),
                    HttpCookie.getCommentWithoutAttributes(comment),
                    0,
                    HttpCookie.getSameSiteFromComment(comment));
        }
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return cookieConfig;
    }

    /**
     * Custom SessionCookieConfig that gets values from environment / config file.
     */
    public class MangoSessionCookieConfig implements SessionCookieConfig {
        @Override
        public void setName(String name) {
        }

        @Override
        public String getName() {
            return Common.getCookieName();
        }

        @Override
        public void setDomain(String domain) {
        }

        @Override
        public String getDomain() {
            String cookieDomain = env.getProperty("sessionCookie.domain");
            return StringUtil.isEmpty(cookieDomain) ? null : cookieDomain;
        }

        @Override
        public void setPath(String path) {
        }

        @Override
        public String getPath() {
            String sessionPath = env.getProperty("sessionCookie.path");
            return StringUtil.isEmpty(sessionPath) ? null : sessionPath;
        }

        @Override
        public void setComment(String comment) {
        }

        @Override
        public String getComment() {
            return env.getProperty("sessionCookie.comment");
        }

        @Override
        public void setHttpOnly(boolean httpOnly) {
        }

        @Override
        public boolean isHttpOnly() {
            return true;
        }

        @Override
        public void setSecure(boolean secure) {
        }

        @Override
        public boolean isSecure() {
            return env.getProperty("sessionCookie.secure", Boolean.class, false);
        }

        @Override
        public void setMaxAge(int maxAge) {
        }

        @Override
        public int getMaxAge() {
            return env.getProperty("sessionCookie.maxAge", Integer.class);
        }
    }
}
