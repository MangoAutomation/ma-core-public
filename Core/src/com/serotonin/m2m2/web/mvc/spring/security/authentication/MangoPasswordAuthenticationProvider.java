/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.security.RateLimiter;


/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoPasswordAuthenticationProvider implements AuthenticationProvider {
    private final Log log = LogFactory.getLog(MangoPasswordAuthenticationProvider.class);

    // throwing a standard AuthenticationException results in the providerManager trying additional providers
    // throw a subclass of AccountStatusException instead
    private static class RateLimitExceededException extends AccountStatusException {
        private static final long serialVersionUID = 1L;

        public RateLimitExceededException() {
            super("Rate limit exceeded");
        }
    }

    private final UserDetailsService userDetailsService;
    private final UserDetailsChecker userDetailsChecker;
    /**
     * Limits the rate at which authentication attempts can occur by an IP
     */
    private final RateLimiter<String> ipRateLimiter;
    /**
     * Limits the rate at which authentication attempts can occur against a username
     */
    private final RateLimiter<String> usernameRateLimiter;
    /**
     * Stores a boolean to indicate if an attack by an IP was already logged, stops the logs being flooded.
     * Will reset after 5 minutes of no attacks being detected.
     */
    private final Cache<String, Boolean> ipLogged;
    /**
     * Stores a boolean to indicate if an attack against a username was already logged, stops the logs being flooded.
     * Will reset after 5 minutes of no attacks being detected.
     */
    private final Cache<String, Boolean> usernameLogged;

    @SuppressWarnings("deprecation")
    @Autowired
    public MangoPasswordAuthenticationProvider(UserDetailsService userDetailsService, UserDetailsChecker userDetailsChecker) {
        this.userDetailsService = userDetailsService;
        this.userDetailsChecker = userDetailsChecker;

        if (Common.envProps.getBoolean("rateLimit.authentication.ip.enabled", true)) {
            this.ipRateLimiter = new RateLimiter<>(
                    Common.envProps.getLong("rateLimit.authentication.ip.burstQuantity", 5),
                    Common.envProps.getLong("rateLimit.authentication.ip.quanitity", 1),
                    Common.envProps.getLong("rateLimit.authentication.ip.period", 1),
                    Common.envProps.getTimeUnitValue("rateLimit.authentication.ip.periodUnit", TimeUnit.MINUTES));
        } else {
            this.ipRateLimiter = null;
        }

        if (Common.envProps.getBoolean("rateLimit.authentication.user.enabled", true)) {
            this.usernameRateLimiter = new RateLimiter<>(
                    Common.envProps.getLong("rateLimit.authentication.user.burstQuantity", 5),
                    Common.envProps.getLong("rateLimit.authentication.user.quanitity", 1),
                    Common.envProps.getLong("rateLimit.authentication.user.period", 1),
                    Common.envProps.getTimeUnitValue("rateLimit.authentication.user.periodUnit", TimeUnit.MINUTES));
        } else {
            this.usernameRateLimiter = null;
        }

        if (log.isWarnEnabled()) {
            this.ipLogged = Caffeine.newBuilder()
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();
            this.usernameLogged = Caffeine.newBuilder()
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();
        } else {
            this.ipLogged = null;
            this.usernameLogged = null;
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.security.authentication.AuthenticationProvider#authenticate(org.springframework.security.core.Authentication)
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
            return null;
        }

        String username = authentication.getName();

        if (!(authentication.getDetails() instanceof WebAuthenticationDetails)) {
            throw new InternalAuthenticationServiceException("Expected authentication details to be instance of WebAuthenticationDetails");
        }

        WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
        String ip = details.getRemoteAddress();

        boolean ipRateExceeded = this.ipRateLimiter != null && this.ipRateLimiter.checkRateExceeded(ip);
        boolean usernameRateExceeded = this.usernameRateLimiter != null && this.usernameRateLimiter.checkRateExceeded(username);

        if (ipRateExceeded) {
            if (log.isWarnEnabled() && ipLogged != null && ipLogged.getIfPresent(ip) != Boolean.TRUE) {
                ipLogged.put(ip, Boolean.TRUE);
                log.warn("Possible brute force attack, IP address " + ip + " exceeded rate limit of " + this.ipRateLimiter.getRefillQuanitity() + " authentication attempts per " + this.ipRateLimiter.getRefillPeriod() + " " + this.ipRateLimiter.getRefillPeriodUnit());
            }
            throw new RateLimitExceededException();
        }

        if (usernameRateExceeded) {
            if (log.isWarnEnabled() && usernameLogged != null && usernameLogged.getIfPresent(username) != Boolean.TRUE) {
                usernameLogged.put(username, Boolean.TRUE);
                log.warn("Possible brute force attack, against username " + username + ", exceeded rate limit of " + this.ipRateLimiter.getRefillQuanitity() + " authentication attempts per " + this.ipRateLimiter.getRefillPeriod() + " " + this.ipRateLimiter.getRefillPeriodUnit());
            }
            throw new RateLimitExceededException();
        }

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
        this.userDetailsChecker.check(userDetails);

        // Validating the password against the database.
        if (!Common.checkPassword((String) authentication.getCredentials(), userDetails.getPassword())) {
            throw new BadCredentialsException(Common.translate("login.validation.invalidLogin"));
        }

        if (!(userDetails instanceof User)) {
            throw new InternalAuthenticationServiceException("Expected user details to be instance of User");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), Collections.unmodifiableCollection(userDetails.getAuthorities()));
    }

    /* (non-Javadoc)
     * @see org.springframework.security.authentication.AuthenticationProvider#supports(java.lang.Class)
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public static UsernamePasswordAuthenticationToken createAuthenticatedToken(User user) {
        //Set User object as the Principle in our Token
        return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
    }
}
