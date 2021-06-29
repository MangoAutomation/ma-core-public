/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp.filters;

import javax.servlet.DispatcherType;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.ForwardedHeaderFilter;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.webapp.TrustForwardedMatcher;

/**
 *
 * <p>Cannot use {@link org.springframework.web.filter.ForwardedHeaderFilter ForwardedHeaderFilter} until <a href="https://github.com/spring-projects/spring-framework/issues/23260">issue is resolved</a>.
 * Namely it does not support using the X-Forwarded-For header to override getRemoteAddr().</p>
 *
 * <p>Use {@link com.infiniteautomation.mango.spring.webapp.MangoForwardedRequestCustomizer MangoForwardedRequestCustomizer} instead.</p>
 *
 * @author Jared Wiltshire
 */
//@Component
@ConditionalOnProperty("${web.forwardedHeaders.enabled:true}")
@WebFilter(
        filterName = MangoForwardedHeaderFilter.NAME,
        asyncSupported = true,
        urlPatterns = {"/*"},
        dispatcherTypes = {DispatcherType.REQUEST})
@Order(FilterOrder.FORWARDED_HEADER)
public class MangoForwardedHeaderFilter extends ForwardedHeaderFilter {
    public static final String NAME = "mangoForwardedHeaderFilter";

    private final RequestMatcher matcher;

    @Autowired
    public MangoForwardedHeaderFilter(TrustForwardedMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (super.shouldNotFilter(request)) {
            return true;
        }

        return !this.matcher.matches(request);
    }

}