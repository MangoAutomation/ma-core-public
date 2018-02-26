/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.exception;

import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;

/**
 * 
 * @author Terry Packer
 */
public class ExceptionUtils {

	/**
	 * Log a web exception in a standard format
	 * @param ex
	 * @param request
	 * @param log
	 */
	public static void logWebException(Throwable ex, HttpServletRequest request, Log log){
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

		// Write the request attributes.
		sw.append("\r\n\r\nREQUEST ATTRIBUTES\r\n");
		names = request.getAttributeNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			sw.append("   ").append(name).append('=').append(String.valueOf(request.getAttribute(name))).append("\r\n");
		}

		log.warn(sw.toString(), ex);
	}
}
