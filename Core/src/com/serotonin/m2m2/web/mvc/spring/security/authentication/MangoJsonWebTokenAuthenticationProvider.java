/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.UserDao;
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

    @Autowired
    public MangoJsonWebTokenAuthenticationProvider(UserAuthJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof BearerAuthenticationToken)) {
            return null;
        }
        
        String bearerToken = (String) authentication.getCredentials();

        Jws<Claims> claims;
        String username;
        Number userId;
        
        // decode
        try {
            claims = jwtService.parse(bearerToken);
            username = claims.getBody().getSubject();
            userId = (Number) claims.getBody().get("id");
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

        User user = UserDao.instance.getUser(username);
        if (user == null)
            throw new UsernameNotFoundException(Common.translate("login.validation.invalidLogin"));
        
        if (userId == null || !userId.equals(user.getId())) {
            throw new BadCredentialsException("Invalid user id for username");
        }

        if (user.isDisabled())
            throw new DisabledException(Common.translate("login.validation.accountDisabled"));

        return new PreAuthenticatedAuthenticationToken(user, claims, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
