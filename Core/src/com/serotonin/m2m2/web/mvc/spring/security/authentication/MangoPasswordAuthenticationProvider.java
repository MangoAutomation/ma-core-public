/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.security.authentication;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;


/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoPasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final UserDetailsChecker userDetailsChecker;

    @Autowired
    public MangoPasswordAuthenticationProvider(MangoUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        this.userDetailsChecker = new AccountStatusUserDetailsChecker();
    }
    
	/* (non-Javadoc)
	 * @see org.springframework.security.authentication.AuthenticationProvider#authenticate(org.springframework.security.core.Authentication)
	 */
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
	    if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
	        return null;
	    }

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(authentication.getName());
        this.userDetailsChecker.check(userDetails);

        // Validating the password against the database.
        if (!Common.checkPassword((String) authentication.getCredentials(), userDetails.getPassword())) {
            throw new BadCredentialsException(Common.translate("login.validation.invalidLogin"));
        }

        Object principal;
        if (userDetails instanceof User) {
            principal = userDetails;
        } else {
            principal = userDetails.getUsername();
        }

        return new UsernamePasswordAuthenticationToken(principal, userDetails.getPassword(), Collections.unmodifiableCollection(userDetails.getAuthorities()));
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
