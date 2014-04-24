/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.serotonin.m2m2.Common;

/**
 * 
 * Filter for use with the 
 * 
 * <
 * 
 * Allows detection of URLs prefixed with /x.z.z/ where 
 * x - Major Version Number
 * y - Minor Version Number
 * z - Micro Version Number
 * 
 *  Useful for version-ing static resources
 *  
 * @author Terry Packer
 *
 */
public class UrlRewriteFilter implements Filter{

	private String versionString;
	
	/* (non-Javadoc)
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
	}

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {
		
		//Rewrite the URL
        final HttpServletRequest hsRequest = (HttpServletRequest) servletRequest;
        String requestURI = hsRequest.getRequestURI();
		
        //Does this URI require re-writing?
        if(requestURI.startsWith(versionString)){
        	final String newURI = requestURI.replace(versionString, "");
            servletRequest.getRequestDispatcher(newURI).forward(hsRequest, servletResponse);
            return;
        }else{
        	filterChain.doFilter(servletRequest, servletResponse);
        }
	}

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		this.versionString = "/" + Common.getVersion().getFullString();
	}
	
}
