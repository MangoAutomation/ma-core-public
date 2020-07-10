/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsChecker;
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
@Order(2)
@ConditionalOnProperty(value = {"${authentication.token.enabled:true}"})
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
        Jws<Claims> jws;

        try {
            jws = tokenAuthenticationService.parse(bearerToken);
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

        return new JwtAuthentication(user, bearerToken, jws, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
