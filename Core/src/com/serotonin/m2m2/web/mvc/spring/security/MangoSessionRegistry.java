/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import org.springframework.security.core.session.SessionRegistryImpl;

/**
 * 
 * Customize Session Registration Actions and Track Logged In User Counts
 * 
 * @author Terry Packer
 */
public class MangoSessionRegistry extends SessionRegistryImpl {
	 
	
	/* (non-Javadoc)
	 * @see org.springframework.security.core.session.SessionRegistryImpl#registerNewSession(java.lang.String, java.lang.Object)
	 */
	@Override
	public void registerNewSession(String sessionId, Object principal) {
		super.registerNewSession(sessionId, principal);
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.session.SessionRegistryImpl#removeSessionInformation(java.lang.String)
	 */
	@Override
	public void removeSessionInformation(String sessionId) {
		super.removeSessionInformation(sessionId);
	}

}
