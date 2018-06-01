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
    private final RateLimiter<String> authenticationRateLimiter;

    @SuppressWarnings("deprecation")
    @Autowired
    public MangoPasswordAuthenticationProvider(UserDetailsService userDetailsService, UserDetailsChecker userDetailsChecker) {
        this.userDetailsService = userDetailsService;
        this.userDetailsChecker = userDetailsChecker;

        this.authenticationRateLimiter = new RateLimiter<>(
                Common.envProps.getLong("rateLimit.authentication.burstCapacity", 5),
                Common.envProps.getLong("rateLimit.authentication.refillQuanitity", 1),
                Common.envProps.getLong("rateLimit.authentication.refillPeriod", 30),
                Common.envProps.getTimeUnitValue("rateLimit.authentication.refillPeriodUnit", TimeUnit.SECONDS));
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

        if (this.authenticationRateLimiter.checkRateExceeded(username)) {
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
