/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.webapp;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.infiniteautomation.mango.webapp.TrustForwardedMatcher;

/**
 * <p>Cannot use {@link com.infiniteautomation.mango.webapp.filters.MangoForwardedHeaderFilter MangoForwardedHeaderFilter} until <a href="https://github.com/spring-projects/spring-framework/issues/23260">issue is resolved</a>.
 * Namely it does not support using the X-Forwarded-For header to override getRemoteAddr().</p>
 * @author Jared Wiltshire
 */
public class MangoForwardedRequestCustomizer extends ForwardedRequestCustomizer {

    private final RequestMatcher matcher;

    public MangoForwardedRequestCustomizer(TrustForwardedMatcher matcher) {
        super();
        this.matcher = matcher;
    }

    @Override
    public void customize(Connector connector, HttpConfiguration config, Request request) {
        if (this.matcher.matches(request)) {
            super.customize(connector, config, request);
        }
    }

}
