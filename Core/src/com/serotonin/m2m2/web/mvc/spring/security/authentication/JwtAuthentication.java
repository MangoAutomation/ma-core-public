/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import com.serotonin.m2m2.vo.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/**
 * @author Jared Wiltshire
 */
public class JwtAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final User principal;
    private final String credentials;
    private final Jws<Claims> token;

    public JwtAuthentication(User aPrincipal, String aCredentials, Jws<Claims> token,
            Collection<? extends GrantedAuthority> anAuthorities) {
        super(anAuthorities);
        this.principal = aPrincipal;
        this.credentials = aCredentials;
        this.token = token;
        setAuthenticated(true);
    }

    /**
     * Get the credentials
     */
    @Override
    public String getCredentials() {
        return this.credentials;
    }

    /**
     * Get the principal
     */
    @Override
    public User getPrincipal() {
        return this.principal;
    }

    public Jws<Claims> getToken() {
        return token;
    }

}
