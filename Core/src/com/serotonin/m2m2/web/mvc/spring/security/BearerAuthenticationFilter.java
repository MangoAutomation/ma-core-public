/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import com.serotonin.m2m2.web.mvc.spring.security.authentication.BearerAuthenticationToken;

/**
 * Attempts authentication based on a bearer Authorization header
 * Based loosely on {@link org.springframework.security.web.authentication.www.BasicAuthenticationFilter}
 *
 * @author Jared Wiltshire
 */
public class BearerAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AuthenticationManager authenticationManager;
    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

    public BearerAuthenticationFilter(AuthenticationManager authenticationManager,
            AuthenticationEntryPoint authenticationEntryPoint,
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        Assert.notNull(authenticationEntryPoint,
                "authenticationEntryPoint cannot be null");
        Assert.notNull(authenticationDetailsSource,
                "authenticationDetailsSource cannot be null");
        this.authenticationManager = authenticationManager;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.authenticationManager,
                "An AuthenticationManager is required");

        Assert.notNull(this.authenticationEntryPoint,
                "An AuthenticationEntryPoint is required");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String tokenString = null;

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            tokenString = header.substring(7).trim();
        }

        if (tokenString == null || tokenString.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (authenticationIsRequired()) {
                BearerAuthenticationToken authRequest = new BearerAuthenticationToken(tokenString);
                authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
                Authentication authResult = authenticationManager.authenticate(authRequest);
                SecurityContextHolder.getContext().setAuthentication(authResult);
            }
        } catch (AuthenticationException failed) {
            SecurityContextHolder.clearContext();

            // we could handle this with the authentication entry point, however this causes anyone using a token
            // injected via a proxy to be continually be redirected to the login page in an infinite loop
            // (usually its impossible for a browser request to have a Authentication Bearer header)
            //this.authenticationEntryPoint.commence(request, response, failed);

            // we could carry on and pretend nothing happened
            //chain.doFilter(request, response);

            // instead lets just send a HTTP 401
            response.sendError(HttpStatus.UNAUTHORIZED.value(), failed.getMessage());

            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Pulled from BasicAuthenticationFilter, checks if the user is already authenticated
     *
     * @param username
     * @return
     */
    private boolean authenticationIsRequired() {
        // Only reauthenticate if username doesn't match SecurityContextHolder and user
        // isn't authenticated
        // (see SEC-53)
        Authentication existingAuth = SecurityContextHolder.getContext()
                .getAuthentication();

        if (existingAuth == null || !existingAuth.isAuthenticated()) {
            return true;
        }

        // Handle unusual condition where an AnonymousAuthenticationToken is already
        // present
        // This shouldn't happen very often, as BasicProcessingFitler is meant to be
        // earlier in the filter
        // chain than AnonymousAuthenticationFilter. Nevertheless, presence of both an
        // AnonymousAuthenticationToken
        // together with a BASIC authentication request header should indicate
        // reauthentication using the
        // BASIC protocol is desirable. This behaviour is also consistent with that
        // provided by form and digest,
        // both of which force re-authentication if the respective header is detected (and
        // in doing so replace
        // any existing AnonymousAuthenticationToken). See SEC-610.
        if (existingAuth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        return false;
    }
}
