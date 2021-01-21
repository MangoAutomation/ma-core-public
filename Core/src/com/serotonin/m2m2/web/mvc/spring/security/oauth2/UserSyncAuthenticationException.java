/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.web.mvc.spring.security.oauth2;

import org.springframework.security.core.AuthenticationException;

public class UserSyncAuthenticationException extends AuthenticationException {
    public UserSyncAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserSyncAuthenticationException(String msg) {
        super(msg);
    }
}
