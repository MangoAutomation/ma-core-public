/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

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

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.security.RateLimiter;


/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoPasswordAuthenticationProvider implements AuthenticationProvider {

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
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();

        WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
        String ip = details.getRemoteAddress();

        try {
            if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
                return null;
            }

            if (!(authentication.getDetails() instanceof WebAuthenticationDetails)) {
                throw new InternalAuthenticationServiceException("Expected authentication details to be instance of WebAuthenticationDetails");
            }

            boolean ipRateExceeded = this.ipRateLimiter != null && this.ipRateLimiter.checkRateExceeded(ip);
            boolean usernameRateExceeded = this.usernameRateLimiter != null && this.usernameRateLimiter.checkRateExceeded(username);

            if (ipRateExceeded) {
                throw new IpAddressAuthenticationRateException();
            }

            if (usernameRateExceeded) {
                throw new UsernameAuthenticationRateException();
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
        } catch (AuthenticationException e) {
            if (this.ipRateLimiter != null) {
                this.ipRateLimiter.hit(ip);
            }
            if (this.usernameRateLimiter != null) {
                this.usernameRateLimiter.hit(username);
            }
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public static UsernamePasswordAuthenticationToken createAuthenticatedToken(User user) {
        //Set User object as the Principle in our Token
        return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
    }

    public static class IpAddressAuthenticationRateException extends AccountStatusException {
        private static final long serialVersionUID = 1L;

        public IpAddressAuthenticationRateException() {
            super("Authentication attempt rate limit exceeded for IP");
        }
    }

    public static class UsernameAuthenticationRateException extends AccountStatusException {
        private static final long serialVersionUID = 1L;

        public UsernameAuthenticationRateException() {
            super("Authentication attempt rate limit exceeded against username");
        }
    }
}
