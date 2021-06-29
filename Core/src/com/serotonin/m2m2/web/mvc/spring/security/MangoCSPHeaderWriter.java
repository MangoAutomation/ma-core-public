/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @author Jared Wiltshire
 */
public class MangoCSPHeaderWriter implements HeaderWriter {

    private final String headerName;
    private String policy;
    private RequestMatcher matcher;

    public MangoCSPHeaderWriter(boolean reportOnly, List<String> policies, RequestMatcher matcher) {
        this.headerName = reportOnly ? "Content-Security-Policy-Report-Only" : "Content-Security-Policy";

        if (matcher == null) {
            this.matcher = matcher;
        } else {
            this.matcher = new AndRequestMatcher(BrowserRequestMatcher.INSTANCE, matcher);
        }

        this.policy = String.join("; ", policies);
    }

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        if (this.matcher == null || this.matcher.matches(request)) {
            response.setHeader(headerName, policy);
        }
    }
}