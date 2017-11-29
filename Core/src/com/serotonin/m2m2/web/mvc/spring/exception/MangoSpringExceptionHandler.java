/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.exception;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.infiniteautomation.mango.rest.v2.exception.AbstractRestV2Exception;
import com.infiniteautomation.mango.rest.v2.exception.AccessDeniedException;
import com.infiniteautomation.mango.rest.v2.exception.GenericRestException;
import com.infiniteautomation.mango.rest.v2.exception.NotFoundRestException;
import com.infiniteautomation.mango.rest.v2.exception.ResourceNotFoundException;
import com.infiniteautomation.mango.rest.v2.exception.ServerErrorException;
import com.infiniteautomation.mango.rest.v2.exception.ValidationFailedRestException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.exception.NotFoundException;
import com.serotonin.m2m2.vo.exception.ValidationException;
import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * 
 * @author Terry Packer
 */
@ControllerAdvice
public class MangoSpringExceptionHandler extends ResponseEntityExceptionHandler{

	private final Log LOG = LogFactory.getLog(MangoSpringExceptionHandler.class);
	
	final RequestMatcher browserHtmlRequestMatcher;
	
	@Autowired
	public MangoSpringExceptionHandler(@Qualifier("browserHtmlRequestMatcher") RequestMatcher browserHtmlRequestMatcher) {
		this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
	}
	
	//Anything that extends our Base Exception
    @ExceptionHandler({AbstractRestV2Exception.class})
    protected ResponseEntity<Object> handleMangoError(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req) {
    	    //Since all Exceptions handled by this method extend AbstractRestV2Exception we don't need to check type
    	    AbstractRestV2Exception e = (AbstractRestV2Exception)ex;
    	    return handleExceptionInternal(ex, ex, new HttpHeaders(), e.getStatus(), req);
    }
    
    //TODO Handle Permission Exception Here

	private final String ACCESS_DENIED = "/exception/accessDenied.jsp";
	@SuppressWarnings("unused")
	// XXX Previous code used this page if it was a CSRF exception, but this is not really an invalid session
    private final String INVALID_SESSION = "/exception/invalidSession.jsp";
	
    @ExceptionHandler({
        org.springframework.security.access.AccessDeniedException.class,
    	    PermissionException.class
    	})
    public ResponseEntity<Object> handleAccessDenied(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req){
        Object model;
        
        if (ex instanceof PermissionException) {
            PermissionException permissionException = (PermissionException) ex;
            model = new AccessDeniedException(permissionException.getTranslatableMessage(), ex);
        } else {
            model = new AccessDeniedException(ex);
        }
        
        return handleExceptionInternal(ex, model, new HttpHeaders(), HttpStatus.FORBIDDEN, req);
    }
    
    @ExceptionHandler({
        ValidationException.class
    })
    public ResponseEntity<Object> handleValidationException(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req) {
        ValidationException validationException = (ValidationException) ex;
        return handleExceptionInternal(ex, new ValidationFailedRestException(validationException.getValidationResult()), new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, req);
    }
    
    @ExceptionHandler({
        NotFoundException.class,
        ResourceNotFoundException.class
    })
    public ResponseEntity<Object> handleNotFoundException(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req) {
        return handleExceptionInternal(ex, new NotFoundRestException(ex), new HttpHeaders(), HttpStatus.NOT_FOUND, req);
    }
        
    @ExceptionHandler({
    	Exception.class
    })
    public ResponseEntity<Object> handleAllOtherErrors(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req){
        return handleExceptionInternal(ex, new ServerErrorException(ex), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, req);
    }
    
    /* (non-Javadoc)
     * @see org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler#handleExceptionInternal(java.lang.Exception, java.lang.Object, org.springframework.http.HttpHeaders, org.springframework.http.HttpStatus, org.springframework.web.context.request.WebRequest)
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
    		Object body, HttpHeaders headers, HttpStatus status,
    		WebRequest request) {
    	
        	HttpServletRequest servletRequest = ((ServletWebRequest)request).getRequest();
        	HttpServletResponse servletResponse = ((ServletWebRequest)request).getResponse();
        	
        	this.storeException(servletRequest, ex, status);
        	
        	if(this.browserHtmlRequestMatcher.matches(servletRequest)){
            String uri;
            if (status == HttpStatus.FORBIDDEN) {
                // browser HTML request
                uri = ACCESS_DENIED;

                User user = Common.getHttpUser();
                if (user != null) {
                    uri = DefaultPagesDefinition.getUnauthorizedUri(servletRequest, servletResponse,
                            user);
                }

                // Put exception into request scope (perhaps of use to a view)
                servletRequest.setAttribute(WebAttributes.ACCESS_DENIED_403, ex);

                // Set the 403 status code.
                servletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        		}else{
    	    		    uri = DefaultPagesDefinition.getErrorUri(servletRequest, servletResponse);
        		}
        		try {
        		    servletResponse.sendRedirect(uri);
    			} catch (IOException e) {
    				LOG.error(e.getMessage(), e);
    			}
        		return null;
        	}else{
        	    //To strip off the double messages generated by this...
            if(ex instanceof NestedRuntimeException)
            	ex = (Exception) ((NestedRuntimeException) ex).getMostSpecificCause();
    
        	    //If no body provided we will create one 
            if(body == null)
                body = new GenericRestException(status, ex);
            
            return new ResponseEntity<Object>(body, headers, status);    		
        	}
    }
    
    /**
     * Store the exception into the session if one exists
     * 
     * @param request
     * @param ex
     */
    protected void storeException(HttpServletRequest request, Exception ex, HttpStatus status){
    	    // Set Exception into Context
		HttpSession sesh = request.getSession(false);
		if (sesh != null)
			sesh.setAttribute(Common.SESSION_USER_EXCEPTION, ex);
        //Log all but not found exceptions
        if(!status.equals(HttpStatus.NOT_FOUND))
            ExceptionUtils.logWebException(ex, request, LOG);
    }
    
}
