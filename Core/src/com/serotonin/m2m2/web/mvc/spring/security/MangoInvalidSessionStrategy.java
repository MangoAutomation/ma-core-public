/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;

/**
 * Determines the type of requestion Web/REST and then responds 
 * with a Re-direct (Web) or 401 Invalid Session (REST) 
 * @author Terry Packer
 */
public class MangoInvalidSessionStrategy implements InvalidSessionStrategy{

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final RequestMatcher browserHtmlRequestMatcher;

	
	@Autowired
	public MangoInvalidSessionStrategy(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher){
		this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.security.web.session.InvalidSessionStrategy#onInvalidSessionDetected(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		
		if (browserHtmlRequestMatcher.matches(request)) {
			
			String uri;
			User user = Common.getHttpUser();
		    if (user != null) {
                uri = DefaultPagesDefinition.getUnauthorizedUri(request, response, user);
            }else{
				//From Web Request, create new session do redirect
				uri = DefaultPagesDefinition.getLoginUri(request, response);
            }
			
			request.getSession();  //Purposefully create a new session
			uri = UriComponentsBuilder.fromUriString(uri)
	                .build()
	                .toUriString();
			redirectStrategy.sendRedirect(request, response, uri);
		}else {
			//Send JSON Response FOR Non Browser Requests
			response.sendError(HttpStatus.UNAUTHORIZED_401, "Invalid Session");
        }
	}
}
