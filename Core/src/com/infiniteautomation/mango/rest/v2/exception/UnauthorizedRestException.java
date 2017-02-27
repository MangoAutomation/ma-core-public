/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * 
 * Exception when a User tries to access a restricted endpoint and they are not logged in
 * 
 * @author Terry Packer
 */
public class UnauthorizedRestException extends AbstractRestV2Exception{

	private static final long serialVersionUID = 1L;
	
	public UnauthorizedRestException(){
		super(HttpStatus.UNAUTHORIZED, null, new TranslatableMessage("login.pleaseSignIn"));
	}

}
