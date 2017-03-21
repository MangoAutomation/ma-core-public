/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

import com.infiniteautomation.mango.rest.v2.exception.UnauthorizedRestException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

/**
 * 
 * @author Terry Packer
 */
public class MangoSwitchUserFilter extends SwitchUserFilter{

	/* (non-Javadoc)
	 * @see org.springframework.security.web.authentication.switchuser.SwitchUserFilter#attemptSwitchUser(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Authentication attemptSwitchUser(HttpServletRequest request) throws AuthenticationException {
		User user = Common.getHttpUser();
		if(!user.isAdmin())
			throw new UnauthorizedRestException();
		return super.attemptSwitchUser(request);
	}
}
