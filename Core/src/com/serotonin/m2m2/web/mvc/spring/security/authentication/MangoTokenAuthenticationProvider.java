/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.ConditionalOnProperty;
import com.infiniteautomation.mango.spring.components.TokenAuthenticationService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.vo.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

/**
 * @author Jared Wiltshire
 */
@Component
@Order(3)
@ConditionalOnProperty(value = {"${authentication.token.enabled}", "${authentication.mango-jwt.enabled}"})
public class MangoTokenAuthenticationProvider implements AuthenticationProvider {

    private final Logger log = LoggerFactory.getLogger(MangoTokenAuthenticationProvider.class);
    private final TokenAuthenticationService tokenAuthenticationService;
    private final UserDetailsChecker userDetailsChecker;

    @Autowired
    public MangoTokenAuthenticationProvider(TokenAuthenticationService jwtService, UserDetailsChecker userDetailsChecker) {
        this.tokenAuthenticationService = jwtService;
        this.userDetailsChecker = userDetailsChecker;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof BearerTokenAuthenticationToken)) return null;
        BearerTokenAuthenticationToken bearerToken = (BearerTokenAuthenticationToken) authentication;

        User user;
        Jws<Claims> jws;

        try {
            jws = tokenAuthenticationService.parse(bearerToken.getToken());
            user = tokenAuthenticationService.verify(jws);
        } catch (ExpiredJwtException e) {
            throw new CredentialsExpiredException("JWT token expired", e);
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            // assume that this is not a JWT, allow the next AuthenticationProvider to process it
            return null;
        } catch (SignatureException | MissingClaimException | IncorrectClaimException e) {
            throw new BadCredentialsException("JWT signature verification error or claim incorrect", e);
        } catch (NotFoundException e) {
            throw new BadCredentialsException("Invalid username", e);
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException("Error authenticating with JWT token", e);
        }

        userDetailsChecker.check(user);

        if (log.isDebugEnabled()) {
            log.debug("Successfully authenticated user using JWT token, header: " + jws.getHeader() + ", body: " + jws.getBody());
        }

        return new JwtAuthentication(user, bearerToken.getToken(), jws, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
