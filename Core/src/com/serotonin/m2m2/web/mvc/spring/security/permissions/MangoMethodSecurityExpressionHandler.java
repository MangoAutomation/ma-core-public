/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.security.permissions;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

import com.infiniteautomation.mango.spring.service.PermissionService;

/**
 * Custom Expression Handler for our Custom EL Security Expressions
 * 
 * 
 * @author Terry Packer
 */
public class MangoMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {
	
    private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private final PermissionService permissionService;
    
    public MangoMethodSecurityExpressionHandler(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
    
	@Override
	protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication,
			MethodInvocation invocation) {
		MangoCustomMethodSecurityExpressionRoot root = new MangoCustomMethodSecurityExpressionRoot(authentication, permissionService);
		root.setPermissionEvaluator(getPermissionEvaluator());
		root.setTrustResolver(this.trustResolver);
		root.setRoleHierarchy(getRoleHierarchy());
		return root;
	}

}
