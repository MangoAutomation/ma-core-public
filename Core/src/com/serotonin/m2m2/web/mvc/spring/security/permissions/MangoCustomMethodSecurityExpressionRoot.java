/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.security.permissions;

import java.util.Set;

import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * Class to define Custom Spring EL Expressions for use in @PreAuthorize and @PostAuthorize annotations
 * 
 * 
 * @author Terry Packer
 */
public class MangoCustomMethodSecurityExpressionRoot extends SecurityExpressionRoot
		implements MethodSecurityExpressionOperations {

	public MangoCustomMethodSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

	/**
	 * Is this User an admin?
	 * @return
	 */
	public boolean isAdmin() {
		User user = (User) this.getPrincipal();
		return user.isAdmin();
	}
	
	/**
	 * Does this User have data source permission?
	 * @return
	 */
	public boolean hasDataSourcePermission(){
		User user =  (User) this.getPrincipal();
		return user.isDataSourcePermission();
	}
	
	/**
	 * Does the user have every one of the supplied permissions
	 * @param permissions
	 * @return
	 */
	public boolean hasAllPermissions(String...permissions){
		User user =  (User) this.getPrincipal();
		Set<String> userPermissions = Permissions.explodePermissionGroups(user.getPermissions());
		//TODO Use Union (See Permissions)
		for(String permission : permissions){
			if(!userPermissions.contains(permission))
				return false;
		}
		return true;
	}
	
	/**
	 * Does the user have at least 1 of the supplied permissions
	 * @param permissions
	 * @return
	 */
	public boolean hasAnyPermission(String...permissions){
		User user =  (User) this.getPrincipal();
		Set<String> userPermissions = Permissions.explodePermissionGroups(user.getPermissions());
		//TODO Use Intersect (See Permissions)
		for(String permission : permissions){
			if(userPermissions.contains(permission))
				return true;
		}
		return false;
	}

	private Object filterObject;
	/* (non-Javadoc)
	 * @see org.springframework.security.access.expression.method.MethodSecurityExpressionOperations#setFilterObject(java.lang.Object)
	 */
	@Override
	public void setFilterObject(Object filterObject) {
		this.filterObject = filterObject;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.access.expression.method.MethodSecurityExpressionOperations#getFilterObject()
	 */
	@Override
	public Object getFilterObject() {
		return filterObject;
	}

	private Object returnObject;
	/* (non-Javadoc)
	 * @see org.springframework.security.access.expression.method.MethodSecurityExpressionOperations#setReturnObject(java.lang.Object)
	 */
	@Override
	public void setReturnObject(Object returnObject) {
		this.returnObject = returnObject;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.access.expression.method.MethodSecurityExpressionOperations#getReturnObject()
	 */
	@Override
	public Object getReturnObject() {
		return returnObject;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.access.expression.method.MethodSecurityExpressionOperations#getThis()
	 */
	@Override
	public Object getThis() {
		return this;
	}

}
