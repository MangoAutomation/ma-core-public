/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.permissions;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.service.PermissionService;

/**
 * Custom Expression Handler for our Custom EL Security Expressions
 * 
 * 
 * @author Terry Packer
 */
@Component
public class MangoMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {
	
    private final AuthenticationTrustResolver trustResolver;
    private final PermissionService permissionService;
    private final PermissionEvaluator permissionEvaluator;

    @Autowired
    public MangoMethodSecurityExpressionHandler(PermissionService permissionService, AuthenticationTrustResolver trustResolver, PermissionEvaluator permissionEvaluator) {
        this.permissionService = permissionService;
        this.trustResolver = trustResolver;
        this.permissionEvaluator = permissionEvaluator;
    }
    
	@Override
	protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication,
			MethodInvocation invocation) {
		MangoCustomMethodSecurityExpressionRoot root = new MangoCustomMethodSecurityExpressionRoot(authentication, permissionService);
		root.setPermissionEvaluator(this.permissionEvaluator);
		root.setTrustResolver(this.trustResolver);
		root.setRoleHierarchy(getRoleHierarchy());
		return root;
	}

}
