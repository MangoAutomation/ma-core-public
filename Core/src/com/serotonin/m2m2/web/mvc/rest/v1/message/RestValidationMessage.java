/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.message;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class RestValidationMessage {

	private RestMessageLevel level;
	private TranslatableMessage message;
    private String property;
	
	public RestValidationMessage(){
		super();
	}

	/**
	 * @param status
	 * @param message
	 */
	public RestValidationMessage(TranslatableMessage message, RestMessageLevel level, String propertyName) {
	    this.message = message;
	    this.level = level;
		this.property = propertyName;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

    public RestMessageLevel getLevel() {
        return level;
    }

    public void setLevel(RestMessageLevel level) {
        this.level = level;
    }

    public TranslatableMessage getMessage() {
        return message;
    }

    public void setMessage(TranslatableMessage message) {
        this.message = message;
    }
	
}
