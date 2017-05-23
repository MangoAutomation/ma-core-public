/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.module.DefaultPagesDefinition;

/**
 * @author Jared Wiltshire
 *
 */
@Component
public class MangoAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
	private static final Log LOG = LogFactory.getLog(MangoAuthenticationFailureHandler.class);
	
    RequestMatcher browserHtmlRequestMatcher;

    @Autowired
    public MangoAuthenticationFailureHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher) {
        this.setAllowSessionCreation(false);
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
    }
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

    	if (browserHtmlRequestMatcher.matches(request)) {
            saveExceptionImpl(request, exception);
            
            String uri = DefaultPagesDefinition.getLoginUri(request, response);
            
            uri = UriComponentsBuilder.fromUriString(uri)
                .build()
                .toUriString();
            
            logger.debug("Redirecting to " + uri);
            this.getRedirectStrategy().sendRedirect(request, response, uri);
        } else {
            request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
            // forward the request on to its usual destination (e.g. /rest/v1/login) so the correct response is returned
            request.getRequestDispatcher(request.getRequestURI()).forward(request, response);
            saveExceptionImpl(request, exception);
        }
    }
    
	protected void saveExceptionImpl(HttpServletRequest request,
			AuthenticationException exception) {
		
		if (this.isUseForward()) {
			request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
		}
		else {
			HttpSession session = request.getSession(false);

			if (session != null || this.isAllowSessionCreation()) {
				request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION,
						exception);
	            //Store for use in the Controller
				String username = request.getParameter("username");
				request.getSession().setAttribute("username", username);
		    	LOG.warn("Failed login attempt on user '"
						+ username + "' from IP +"
						+ request.getRemoteAddr());
			}
		}
	}
}
