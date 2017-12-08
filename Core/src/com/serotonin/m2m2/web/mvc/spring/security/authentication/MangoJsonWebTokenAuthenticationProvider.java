/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.components.UserAuthJwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

/**
 * @author Jared Wiltshire
 */
@Component
public class MangoJsonWebTokenAuthenticationProvider implements AuthenticationProvider {
    private final UserAuthJwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserDetailsChecker userDetailsChecker;

    @Autowired
    public MangoJsonWebTokenAuthenticationProvider(UserAuthJwtService jwtService, UserDetailsService userDetailsService, UserDetailsChecker userDetailsChecker) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userDetailsChecker = userDetailsChecker;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof BearerAuthenticationToken)) {
            return null;
        }
        
        String bearerToken = (String) authentication.getCredentials();

        Jws<Claims> claims;
        // decode
        try {
            claims = jwtService.parse(bearerToken);
        } catch (ExpiredJwtException e) {
            throw new CredentialsExpiredException(e.getMessage(), e);
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            // assume that this is not a JWT, allow the next AuthenticationProvider to process it
            return null;
        } catch (SignatureException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }

        String username = claims.getBody().getSubject();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        userDetailsChecker.check(userDetails);
        
        if (!(userDetails instanceof User)) {
            throw new InternalAuthenticationServiceException("Expected user details to be instance of User");
        }
        
        User user = (User) userDetails;

        Integer userId = user.getId();
        if (!userId.equals(claims.getBody().get(UserAuthJwtService.USER_ID_CLAIM))) {
            throw new BadCredentialsException("Invalid user id for username");
        }

        return new PreAuthenticatedAuthenticationToken(user, claims, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
