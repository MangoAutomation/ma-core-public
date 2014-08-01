/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.message;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class RestMessage {

	private HttpStatus status;
	private String message;


	/**
	 * @param unauthorized
	 * @param translatableMessage
	 */
	public RestMessage(HttpStatus status,
			TranslatableMessage message) {
		this.status = status;
		this.message = message.translate(Common.getTranslations());
	}

	public HttpStatus getStatus() {
		return status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	
	
	
}
