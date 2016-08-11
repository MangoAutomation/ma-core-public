/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.authentication;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.vo.User;

/**
 * @author Terry Packer
 *
 */
public class MangoUserAuthenticationProvider implements AuthenticationProvider{

	/* (non-Javadoc)
	 * @see org.springframework.security.authentication.AuthenticationProvider#authenticate(org.springframework.security.core.Authentication)
	 */
	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		
		User u = UserDao.instance.getUser(authentication.getName());
		if(u == null)
			throw new BadCredentialsException(Common.translate("login.validation.invalidLogin"));
		
		if(u.isDisabled())
			throw new DisabledException(Common.translate("login.validation.accountDisabled"));
		
		//Do Login
		u = Common.loginManager.performLogin(authentication.getName(), (String)authentication.getCredentials(), false);
		
		if(u == null)
			throw new BadCredentialsException(Common.translate("login.validation.invalidLogin"));
		
		return createToken(u);

	}

	/* (non-Javadoc)
	 * @see org.springframework.security.authentication.AuthenticationProvider#supports(java.lang.Class)
	 */
	@Override
	public boolean supports(Class<?> authentication) {
		//TODO Expand on this later to support multiple Authentication Types
		return true;
	}

	public static UsernamePasswordAuthenticationToken createToken(User u){
		String [] roles = u.getPermissions().split(",");
		List<GrantedAuthority> permissions = new ArrayList<GrantedAuthority>(roles.length);
		
		for (String role : roles) {
			permissions.add(new SimpleGrantedAuthority(role));
		}
		if(u.isAdmin())
			permissions.add(new SimpleGrantedAuthority("ADMIN"));
		
		//Set User object as the Principle in our Token
		return new UsernamePasswordAuthenticationToken(u, u.getPassword(), permissions);
	}
	
}
