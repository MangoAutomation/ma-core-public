/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.infiniteautomation.mango.web.mvc.rest.v2.model.exception.GenericRestExceptionModel;
import com.infiniteautomation.mango.web.mvc.rest.v2.model.exception.RestExceptionModel;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * 
 * Exception when a User tries to access a restricted endpoint and they are not logged in
 * 
 * @author Terry Packer
 */
public class UnauthorizedRestException extends AbstractRestV2Exception{

	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.web.mvc.rest.v2.exception.AbstractRestV2Exception#getStatus()
	 */
	@Override
	public HttpStatus getStatus() {
		return HttpStatus.UNAUTHORIZED;
	}

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.web.mvc.rest.v2.exception.AbstractRestV2Exception#getBodyModel()
	 */
	@Override
	public RestExceptionModel getBodyModel() {
		//TODO Fix up this translation to be more accurate?
		return new GenericRestExceptionModel(getStatus().value(), new TranslatableMessage("login.pleaseSignIn"));
	}

}
