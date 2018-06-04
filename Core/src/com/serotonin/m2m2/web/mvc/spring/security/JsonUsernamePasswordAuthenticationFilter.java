/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Attempts authentication for a POST request with a JSON body
 *
 * @author Jared Wiltshire
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class JsonUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String USERNAME_ATTRIBUTE = "MANGO_AUTHENTICATION_USERNAME";

    private ObjectMapper mapper;

    @Autowired
    protected JsonUsernamePasswordAuthenticationFilter(ObjectMapper mapper) {
        super(new AntPathRequestMatcher("/rest/*/login", "POST"));
        this.mapper = mapper;
    }

    @Override
    public void afterPropertiesSet() {
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String contentType = request.getContentType();
        if (contentType == null || contentType.isEmpty())
            return false;

        boolean pathMatches = super.requiresAuthentication(request, response);
        if (!pathMatches) {
            return false;
        }

        MediaType requestMediaType;
        try {
            requestMediaType = MediaType.valueOf(contentType);
        } catch (Exception e) {
            return false;
        }

        return requestMediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        UsernameAndPassword usernameAndPassword = mapper.readValue(request.getInputStream(), UsernameAndPassword.class);

        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }

        String username = usernameAndPassword.getUsername();
        String password = usernameAndPassword.getPassword();
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
        username = username.trim();

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                username, password);

        // store the username so we can access it in the failure handler
        request.setAttribute(USERNAME_ATTRIBUTE, username);

        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    public static class UsernameAndPassword {
        String username;
        String password;
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
