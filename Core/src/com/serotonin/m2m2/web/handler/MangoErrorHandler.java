/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import com.serotonin.m2m2.module.DefaultPagesDefinition;

/**
 * Handle and process some of the basic responses that modules may want to override
 * 
 * @author Terry Packer
 */
public class MangoErrorHandler extends ErrorHandler{

	
/* (non-Javadoc)
 * @see org.eclipse.jetty.server.handler.ErrorHandler#generateAcceptableResponse(org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int, java.lang.String, java.lang.String)
 */
@Override
protected void generateAcceptableResponse(Request baseRequest, HttpServletRequest request,
		HttpServletResponse response, int code, String message, String mimeType) throws IOException {
	
	switch(code){
	case HttpStatus.SC_NOT_FOUND:
		//Search the default pages definitions
		response.sendRedirect(DefaultPagesDefinition.getNotFoundUri(request, response));
	break;
	case HttpStatus.SC_INTERNAL_SERVER_ERROR:
		//Search the default pages definitions
		response.sendRedirect(DefaultPagesDefinition.getErrorUri(request, response));
	break;
	}
}

	
}
