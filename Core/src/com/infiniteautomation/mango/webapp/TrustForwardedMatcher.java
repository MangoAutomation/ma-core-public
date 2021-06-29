/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.webapp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;

/**
 * @author Jared Wiltshire
 */
//@Component
@ConditionalOnProperty("${web.forwardedHeaders.enabled:true}")
public class TrustForwardedMatcher implements RequestMatcher {

    private final List<IpAddressMatcher> ipMatchers;

    @Autowired
    public TrustForwardedMatcher(@Value("${web.forwardedHeaders.trustedIpRanges}") String trustedIpRanges) {
        if (trustedIpRanges == null || trustedIpRanges.isEmpty()) {
            this.ipMatchers = Collections.emptyList();
        } else {
            this.ipMatchers = Arrays.stream(trustedIpRanges.split("\\s*,\\s*")).filter(r -> !r.isEmpty()).map(range -> {
                return new IpAddressMatcher(range);
            }).collect(Collectors.toList());
        }
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return ipMatchers.stream().anyMatch(m -> m.matches(request));
    }

}
