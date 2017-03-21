/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import org.springframework.security.core.session.SessionRegistryImpl;

import com.infiniteautomation.mango.monitor.AtomicIntegerMonitor;
import com.infiniteautomation.mango.monitor.ValueMonitorOwner;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.ILoginManager;

/**
 * 
 * Customize Session Registration Actions and Track Logged In User Counts
 * 
 * @author Terry Packer
 */
public class MangoSessionRegistry extends SessionRegistryImpl implements ILoginManager, ValueMonitorOwner{
	
	 private final AtomicIntegerMonitor sessionCount;
	 
	 public MangoSessionRegistry(){
        this.sessionCount = new AtomicIntegerMonitor(this.getClass().getCanonicalName() + ".COUNT", new TranslatableMessage("internal.monitor.USER_SESSION_COUNT"), this);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(this.sessionCount);
        Common.loginManager = this;
	 }
	
	/* (non-Javadoc)
	 * @see org.springframework.security.core.session.SessionRegistryImpl#registerNewSession(java.lang.String, java.lang.Object)
	 */
	@Override
	public void registerNewSession(String sessionId, Object principal) {
		super.registerNewSession(sessionId, principal);
		this.sessionCount.increment();
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.session.SessionRegistryImpl#removeSessionInformation(java.lang.String)
	 */
	@Override
	public void removeSessionInformation(String sessionId) {
		super.removeSessionInformation(sessionId);
		this.sessionCount.decrement();
	}

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.monitor.ValueMonitorOwner#reset(java.lang.String)
	 */
	@Override
	public void reset(String monitorId) {
		//Nothing to do
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.ILoginManager#getSessionCountMonitor()
	 */
	@Override
	public AtomicIntegerMonitor getSessionCountMonitor() {
		return sessionCount;
	}
}
