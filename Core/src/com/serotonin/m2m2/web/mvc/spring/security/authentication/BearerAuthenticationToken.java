/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * @author Jared Wiltshire
 *
 */
public class BearerAuthenticationToken extends AbstractAuthenticationToken {
    private static final long serialVersionUID = 1L;

    private final String token;
    
    public BearerAuthenticationToken(String token) {
        super(null);
        this.token = token;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

}
