/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.util.Objects;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * @author Jared Wiltshire
 */
public class PasswordResetAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private static final long serialVersionUID = 1L;
    private final String newPassword;

    public PasswordResetAuthenticationToken(Object principal, Object credentials, String newPassword) {
        super(principal, credentials);
        this.newPassword = Objects.requireNonNull(newPassword);
    }

    public String getNewPassword() {
        return newPassword;
    }

}
