/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.ShallowEtagHeaderFilter;

import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 *
 */
public class MangoShallowEtagHeaderFilter extends ShallowEtagHeaderFilter{

	private final String CACHE_CONTROL = "Cache-Control";
	private final String MAX_AGE;
	private final String MUST_REVALIDATE = "must-revalidate";
	private final String COMMA = ",";
	private final String cacheControls;
	
	public MangoShallowEtagHeaderFilter(){
		MAX_AGE = "max-age=" + Common.envProps.getString("web.cache.maxAge", "0");
		StringBuilder builder = new StringBuilder();
		builder.append(MAX_AGE);
		builder.append(COMMA);
		builder.append(MUST_REVALIDATE);
		this.cacheControls = builder.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.web.filter.ShallowEtagHeaderFilter#doFilterInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		//Add the Cache-Control Header
		response.addHeader(CACHE_CONTROL, cacheControls);
		super.doFilterInternal(request, response, filterChain);
	}
	
}
