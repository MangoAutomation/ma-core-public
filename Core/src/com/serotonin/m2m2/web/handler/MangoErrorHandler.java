/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.handler;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.util.NestedServletException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.web.mvc.spring.exception.ExceptionUtils;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;

/**
 * Handle and process some of the basic responses that modules may want to
 * override
 * 
 * @author Terry Packer
 */
public class MangoErrorHandler extends ErrorHandler{

	private final Log LOG = LogFactory.getLog(MangoErrorHandler.class);
	private final String ACCESS_DENIED = "/unauthorized.htm";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jetty.server.handler.ErrorHandler#generateAcceptableResponse(
	 * org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse, int, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	protected void generateAcceptableResponse(Request baseRequest, HttpServletRequest request,
			HttpServletResponse response, int code, String message, String mimeType) throws IOException {
		
		//The cases we need to handle
		// 1) - Not found forwards to not found URI
		// 2) - Exception forwards to error URI
		// 3) - Unauthorized forwards to unauthorized URI
		// 4) - Other ? 
		
		try{
			
			switch (code) {
			case 404:
				if(MangoSecurityConfiguration.browserHtmlRequestMatcher().matches(request)){
					//Forward to Not Found URI
					String uri = DefaultPagesDefinition.getNotFoundUri(request, response);
					RequestDispatcher dispatcher = request.getRequestDispatcher(uri);
	                dispatcher.forward(request, response);
				}else{
					//Resource/Rest Request
					baseRequest.setHandled(true);
				}
				
				break;
			default:
				//Catch All unhandled Responses with errors
				Throwable th = (Throwable)request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
				//Does this require handling
				if(th != null){
					
					if(th instanceof NestedServletException)
						th = th.getCause();
					
					//Log it
					ExceptionUtils.logWebException((Exception)th, request, LOG);
					
					HttpSession sesh = baseRequest.getSession(false);
					String uri;
					
					//We are handling this here
					baseRequest.setHandled(true);
					
					//We need to do something
					if(MangoSecurityConfiguration.browserHtmlRequestMatcher().matches(request)){
						//TODO There is no longer a way to get a PermissionException here due to the PermissionExceptionFilter.  
						// However, there is no User in the Security Context at this point which means we cannot successfully do a forward...
						// need to understand how this is happening.
						// 
						//
						//Are we a PermissionException
						if(th instanceof PermissionException){
							User user = Common.getHttpUser();
							if(user == null)
								uri = ACCESS_DENIED;
							else
								uri = DefaultPagesDefinition.getUnauthorizedUri(request, response, Common.getHttpUser());
							// Put exception into request scope (perhaps of use to a view)
			                request.setAttribute(WebAttributes.ACCESS_DENIED_403, th);
			                response.sendRedirect(uri);
						}else{
							// Redirect to Error URI
							if(sesh != null)
								sesh.setAttribute(Common.SESSION_USER_EXCEPTION, th);
							uri = DefaultPagesDefinition.getErrorUri(baseRequest, response);
							response.sendRedirect(uri);
						}
					}else{
						//Resource/Rest Request
						baseRequest.setHandled(true);
						if(sesh != null)
							sesh.setAttribute(Common.SESSION_USER_EXCEPTION, th.getCause());
					}
				}
				break;
			}
		}catch(ServletException e){
			throw new IOException(e);
		}
	}
}
