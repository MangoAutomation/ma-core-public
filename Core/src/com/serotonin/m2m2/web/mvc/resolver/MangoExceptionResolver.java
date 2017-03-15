/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.resolver;

import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import com.serotonin.m2m2.Common;

/**
 * 
 * Run as the highest priority Exception Resolver 
 * to add the Exception into the Session for later retrieval.
 * 
 * Also does some verbose logging.
 * 
 * @author Terry Packer
 */
public class MangoExceptionResolver extends AbstractHandlerExceptionResolver{

	private static final Log LOG = LogFactory.getLog(MangoExceptionResolver.class);
	
	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver#getOrder()
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) {
		// Set Exception into Context
		HttpSession sesh = request.getSession(false);
		if (sesh != null)
			sesh.setAttribute(Common.SESSION_USER_EXCEPTION, ex);
		
		if(ex != null)
			logException(ex, request);
		
		return null;
	}

	
	/* (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver#shouldApplyTo(javax.servlet.http.HttpServletRequest, java.lang.Object)
	 */
	@Override
	protected boolean shouldApplyTo(HttpServletRequest request, Object handler) {
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver#
	 * logException(java.lang.Exception, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected void logException(Exception ex, HttpServletRequest request) {
		StringWriter sw = new StringWriter();

		// Write the request url into the message.
		sw.append("\r\nREQUEST URL\r\n");
		sw.append(request.getRequestURL());

		// Write the request parameters.
		sw.append("\r\n\r\nREQUEST PARAMETERS\r\n");
		java.util.Enumeration<?> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			sw.append("   ").append(name).append('=').append(request.getParameter(name)).append("\r\n");
		}

		// Write the request headers.
		sw.append("\r\n\r\nREQUEST HEADERS\r\n");
		names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			sw.append("   ").append(name).append('=').append(request.getHeader(name)).append("\r\n");
		}

		// Write the page attributes.
		// sw.append("\r\n\r\nPAGE ATTRIBUTES\r\n");
		// names = pageContext.getAttributeNames();
		// while (names.hasMoreElements()) {
		// String name = (String) names.nextElement();
		// sw.append("
		// ").append(name).append('=').append(pageContext.getAttribute(name)).append("\r\n");
		// }

		// Write the request attributes.
		sw.append("\r\n\r\nREQUEST ATTRIBUTES\r\n");
		names = request.getAttributeNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			sw.append("   ").append(name).append('=').append(String.valueOf(request.getAttribute(name))).append("\r\n");
		}

		HttpSession session = request.getSession(false);
		if (session != null) {

			// Write the session attributes.
			sw.append("\r\n\r\nSESSION ATTRIBUTES\r\n");
			names = session.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				sw.append("   ").append(name).append('=').append(String.valueOf(session.getAttribute(name)))
						.append("\r\n");
			}

			// Write the context attributes.
			sw.append("\r\n\r\nCONTEXT ATTRIBUTES\r\n");
			names = session.getServletContext().getAttributeNames();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();

				String value;
				try {
					value = String.valueOf(session.getServletContext().getAttribute(name));
				} catch (Exception e) {
					value = "EXCEPTION in String.valueOf: " + e.getMessage();
				}

				sw.append("   ").append(name).append('=').append(value).append("\r\n");
			}
		}

		LOG.warn(sw.toString(), ex);
	}

}
