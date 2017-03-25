/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.filter.GenericFilterBean;

import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * Wrap and re-throw PermissionExceptions as AccessDenied Exceptions
 * @author Terry Packer
 */
public class PermissionExceptionFilter extends GenericFilterBean{

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try{
			chain.doFilter(request, response);
		}catch(PermissionException e){
			throw new AccessDeniedException(e.getMessage(), e);
		}
	}

}
