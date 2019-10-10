/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.webapp.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * @author Jared Wiltshire
 */
@Lazy
@Component
@WebFilter(
        filterName = MangoForwardedHeaderFilter.NAME,
        asyncSupported = true,
        urlPatterns = {"/*"})
@Order(-100)
public class MangoForwardedHeaderFilter extends ForwardedHeaderFilter {
    public static final String NAME = "mangoForwardedHeaderFilter";

    private final boolean enabled;
    private final List<IpAddressMatcher> ipMatchers;

    @Autowired
    public MangoForwardedHeaderFilter(@Value("${web.forwardedHeaders.enabled:true}") boolean enabled, @Value("${web.forwardedHeaders.trustedIpRanges}") String trustedIpRanges) {
        this.enabled = enabled;

        if (trustedIpRanges == null || trustedIpRanges.isEmpty()) {
            this.ipMatchers = Collections.emptyList();
        } else {
            this.ipMatchers = Arrays.stream(trustedIpRanges.split("\\s*,\\s*")).filter(r -> !r.isEmpty()).map(range -> {
                return new IpAddressMatcher(range);
            }).collect(Collectors.toList());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (super.shouldNotFilter(request) || !enabled) {
            return true;
        }

        boolean trusted = ipMatchers.stream().anyMatch(m -> m.matches(request));
        return !trusted;
    }

}