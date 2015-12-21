/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

/**
 * @author Terry Packer
 *
 */
public class MangoAccessDeniedHandler implements AccessDeniedHandler{
	
	private final Log LOG = LogFactory.getLog(MangoAccessDeniedHandler.class);

	private final String ACCESS_DENIED = "/exception/accessDenied.jsp";
	private final String INVALID_SESSION = "/exception/invalidSession.jsp";
	private final String REST_PREFIX = "/rest/v1";
	
	/* (non-Javadoc)
	 * @see org.springframework.security.web.access.AccessDeniedHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.security.access.AccessDeniedException)
	 */
	@Override
	public void handle(HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException,
			ServletException {
		
		if (!response.isCommitted()) {
			LOG.warn("Denying access to Mango resource", accessDeniedException);
			// Put exception into request scope (perhaps of use to a view)
			request.setAttribute(WebAttributes.ACCESS_DENIED_403,
					accessDeniedException);
			// Set the 403 status code.
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			RequestDispatcher dispatcher;
			
			//Does this request come from the REST API?  If so don't redirect
			if(!request.getRequestURI().startsWith(REST_PREFIX)){
				if(accessDeniedException instanceof CsrfException){
					// forward to error page.
					dispatcher = request.getRequestDispatcher(INVALID_SESSION);
				}else{
					dispatcher = request.getRequestDispatcher(ACCESS_DENIED);
				}
				dispatcher.forward(request, response);
			}
		}
		else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN,
						accessDeniedException.getMessage());
		}
	}
}