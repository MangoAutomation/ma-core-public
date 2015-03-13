/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.message;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class RestValidationMessage extends BaseRestMessage{

	private String property;
	
	public RestValidationMessage(){
		super();
	}
	
	public RestValidationMessage(String message, RestMessageLevel level, String propertyName){
		super(message, level);
		this.property = propertyName;
	}
	/**
	 * @param status
	 * @param message
	 */
	public RestValidationMessage(TranslatableMessage message, RestMessageLevel level, String propertyName) {
		super(message.translate(Common.getTranslations()), level);
		this.property = propertyName;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}
	
}
