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
public class RestMessage extends BaseRestMessage{

	private HttpStatus status;

	/**
	 * @param status
	 * @param translatableMessage
	 */
	public RestMessage(HttpStatus status,
			TranslatableMessage message) {
		super();
		this.message = message.translate(Common.getTranslations());
		if(status.is1xxInformational()||status.is2xxSuccessful())
			this.level = RestMessageLevel.INFORMATION;
		else if(status.is3xxRedirection())
			this.level = RestMessageLevel.WARNING;
		else if(status.is4xxClientError() || status.is5xxServerError())
			this.level = RestMessageLevel.ERROR;
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}
	
}
