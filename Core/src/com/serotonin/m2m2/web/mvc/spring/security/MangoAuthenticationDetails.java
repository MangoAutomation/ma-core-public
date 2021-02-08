/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class MangoAuthenticationDetails extends WebAuthenticationDetails {

    private final boolean recordLogin;

    public MangoAuthenticationDetails(HttpServletRequest context, boolean recordLogin) {
        super(context);
        this.recordLogin = recordLogin;
    }

    public boolean isRecordLogin() {
        return recordLogin;
    }
}
