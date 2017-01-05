/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;

import com.infiniteautomation.mango.monitor.AtomicIntegerMonitor;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.form.LoginForm;

/**
 * @author Terry Packer
 *
 */
public interface ILoginManager {

    /**
     * Check and store valid ip Addresses
     * @param ip
     * @return
     */
    public boolean isValidIp(HttpServletRequest request);
    

    /**
     * 
     * @param username
     * @param password
     * @param request
     * @param response
     * @param loginForm
     * @param errors
     * @param logout - True to logout current user and replace with new
     * @param passwordEncrypted
     * @return
     * @throws TranslatableException
     */
    public User performLogin(String username, String password, 
    		HttpServletRequest request, HttpServletResponse response,
    		LoginForm loginForm, BindException errors,
    		boolean logout, boolean passwordEncrypted) throws TranslatableException;
    
    /**
     * Simple Login without adding information to HttpSession, no exceptions thrown if user is not 
     * authenticated then null is returned.
     * 
     * NOTE: This does not yet support Authentication via Module Definitions
     * 
     * @param username
     * @param password
     * @param passwordEncrypted
     * @return
     */
    public User performLogin(String username, String password, boolean passwordEncrypted);
    
    /**
     * Does this request contain the urlSecurity attribute?
     * 
     * This means we were referred from a secure page
     * 
     * @param request
     * @return
     */
    public boolean isSecure(HttpServletRequest request);
    
    
    
    /**
     * Simply Logout the User
     * @param request
     * @param response
     */
    public void performLogout(HttpServletRequest request, HttpServletResponse response);
    
    /**
     * Is a user already logged in?
     * @param request
     * @param response
     * @return
     */
    public boolean isLoggedIn(HttpServletRequest request, HttpServletResponse response);
    
    /**
     * Get the monitor for session counts
     * @return
     */
    public AtomicIntegerMonitor getSessionCountMonitor();
	
}
