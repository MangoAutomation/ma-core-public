/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.model.exception;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Generic Model for simple exceptions
 * @author Terry Packer
 */
public class GenericRestExceptionModel extends RestExceptionModel{

	//Code for Error (may be HTTP code or Custom Mango Error Code?)
	private int code;
	private TranslatableMessage message;
	
	/**
	 * @param value
	 * @param translatableMessage
	 */
	public GenericRestExceptionModel(int code, TranslatableMessage message) {
		this.code = code;
		this.message = message;
	}

	public int getCode(){
		return code;
	}
	public String getMessage(){
		return this.message.translate(Common.getTranslations());
	}
}
