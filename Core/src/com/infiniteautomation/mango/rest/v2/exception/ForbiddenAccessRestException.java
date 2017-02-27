/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;

/**
 * Exception when an authorized user tries to access something they do not have privileges to.
 * 
 * @author Terry Packer
 */
public class ForbiddenAccessRestException extends AbstractRestV2Exception{

	private static final long serialVersionUID = 1L;
	private User user;
	
	public ForbiddenAccessRestException(User user){
		super(HttpStatus.FORBIDDEN, new TranslatableMessage("permissions.missing"));
		this.user = user;
	}
	

	public String getUsername() {
		if(user != null)
			return user.getUsername();
		else
			return null;
	}

}
