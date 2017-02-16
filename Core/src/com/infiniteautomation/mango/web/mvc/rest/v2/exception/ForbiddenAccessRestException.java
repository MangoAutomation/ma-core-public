/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.infiniteautomation.mango.web.mvc.rest.v2.model.exception.GenericRestExceptionModel;
import com.infiniteautomation.mango.web.mvc.rest.v2.model.exception.RestExceptionModel;
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
		this.user = user;
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.web.mvc.rest.v2.exception.AbstractRestV2Exception#getStatus()
	 */
	@Override
	public HttpStatus getStatus() {
		return HttpStatus.FORBIDDEN;
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.web.mvc.rest.v2.exception.AbstractRestV2Exception#getBodyModel()
	 */
	@Override
	public RestExceptionModel getBodyModel() {
		return new ForbiddenAccessRestExceptionModel(this);
	}

	class ForbiddenAccessRestExceptionModel extends GenericRestExceptionModel{
		
		private String username;
		
		public ForbiddenAccessRestExceptionModel(ForbiddenAccessRestException exception){
			//TODO Fix up the translation
			super(HttpStatus.FORBIDDEN.value(), new TranslatableMessage("permissions.missing"));
			this.username = exception.user.getUsername();
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}		
	}
}
