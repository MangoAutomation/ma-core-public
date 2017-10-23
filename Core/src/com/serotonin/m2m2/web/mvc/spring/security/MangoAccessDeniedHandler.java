/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;

/**
 * @author Terry Packer
 *
 */
@Component
public class MangoAccessDeniedHandler implements AccessDeniedHandler {
    final RequestMatcher browserHtmlRequestMatcher;

    @Autowired
    public MangoAccessDeniedHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher) {
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
    }
    
	private final Log LOG = LogFactory.getLog(MangoAccessDeniedHandler.class);

	private final String ACCESS_DENIED = "/exception/accessDenied.jsp";
	@SuppressWarnings("unused")
	// XXX Previous code used this page if it was a CSRF exception, but this is not really an invalid session
    private final String INVALID_SESSION = "/exception/invalidSession.jsp";
	
	/* (non-Javadoc)
	 * @see org.springframework.security.web.access.AccessDeniedHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.security.access.AccessDeniedException)
	 */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
	        throws IOException, ServletException {

       LOG.warn("Denying access to Mango resource " + request.getRequestURI() + " to IP " + request.getRemoteAddr(), accessDeniedException);
		if (!response.isCommitted()) {
		    if (browserHtmlRequestMatcher.matches(request)) {
		        // browser HTML request
    		    String accessDeniedUrl = ACCESS_DENIED;
    		    
    		    User user = Common.getHttpUser();
    		    if (user != null) {
    		        LOG.warn("Denied user is " + user.getUsername());
                    accessDeniedUrl = DefaultPagesDefinition.getUnauthorizedUri(request, response, user);
                }
    	        
    	        // Put exception into request scope (perhaps of use to a view)
                request.setAttribute(WebAttributes.ACCESS_DENIED_403, accessDeniedException);
    
                // Set the 403 status code.
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    
                // redirect to error page.
                response.sendRedirect(accessDeniedUrl);
		    } else {
		        // REST type request
		        response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
		    }
		}
	}
}