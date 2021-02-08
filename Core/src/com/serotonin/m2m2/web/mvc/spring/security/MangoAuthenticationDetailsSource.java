/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationDetailsSource;

public class MangoAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, MangoAuthenticationDetails> {

    private final boolean recordLogin;

    public MangoAuthenticationDetailsSource(boolean recordLogin) {
        this.recordLogin = recordLogin;
    }

    @Override
    public MangoAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new MangoAuthenticationDetails(context, recordLogin);
    }

}
