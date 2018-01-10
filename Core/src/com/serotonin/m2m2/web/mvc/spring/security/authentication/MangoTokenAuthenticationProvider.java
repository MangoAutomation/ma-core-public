/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.exception.NotFoundException;
import com.serotonin.m2m2.web.mvc.spring.components.TokenAuthenticationService;

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
public class MangoTokenAuthenticationProvider implements AuthenticationProvider {
    private final TokenAuthenticationService tokenAuthenticationService;
    private final UserDetailsChecker userDetailsChecker;
    private Log log = LogFactory.getLog(MangoTokenAuthenticationProvider.class);

    @Autowired
    public MangoTokenAuthenticationProvider(TokenAuthenticationService jwtService, UserDetailsChecker userDetailsChecker) {
        this.tokenAuthenticationService = jwtService;
        this.userDetailsChecker = userDetailsChecker;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof BearerAuthenticationToken)) {
            return null;
        }
        
        String bearerToken = (String) authentication.getCredentials();

        User user;
        Jws<Claims> claims;
        
        try {
            claims = tokenAuthenticationService.parse(bearerToken);
            user = tokenAuthenticationService.verify(claims);
        } catch (ExpiredJwtException e) {
            throw new CredentialsExpiredException(e.getMessage(), e);
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            // assume that this is not a JWT, allow the next AuthenticationProvider to process it
            return null;
        } catch (SignatureException | MissingClaimException | IncorrectClaimException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        } catch (NotFoundException e) {
            throw new BadCredentialsException("Invalid username", e);
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }

        userDetailsChecker.check(user);
        
        if (log.isDebugEnabled()) {
            log.debug("Successfully authenticated user using JWT token, header: " + claims.getHeader() + ", body: " + claims.getBody());
        }

        return new PreAuthenticatedAuthenticationToken(user, bearerToken, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
