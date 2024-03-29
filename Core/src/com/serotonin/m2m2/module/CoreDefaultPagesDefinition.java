/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;

import com.serotonin.m2m2.vo.User;

/**
 * No UI in core anymore, add a default that just sends everyone to root
 *
 * @author Jared Wiltshire
 */
public class CoreDefaultPagesDefinition extends DefaultPagesDefinition {
    @Override
    public String getLoginPageUri(HttpServletRequest request, HttpServletResponse response) {
        return "/";
    }

    @Override
    public String getLoggedInPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
        return "/";
    }

    @Override
    public String getFirstUserLoginPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
        return "/";
    }

    @Override
    public String getUnauthorizedPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
        return "/";
    }

    @Override
    public String getErrorPageUri(HttpServletRequest request, HttpServletResponse response) {
        return "/";
    }

    @Override
    public String getNotFoundPageUri(HttpServletRequest request, HttpServletResponse response) {
        return "/";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
