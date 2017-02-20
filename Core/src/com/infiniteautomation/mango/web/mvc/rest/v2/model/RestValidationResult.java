/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.model;

import java.util.ArrayList;
import java.util.List;

import com.infiniteautomation.mango.web.mvc.rest.v2.exception.ValidationFailedRestException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestMessageLevel;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestValidationMessage;

/**
 * Container to simplify validation errors
 * 
 * @author Terry Packer
 */
public class RestValidationResult {

	private final List<RestValidationMessage> messages;
	
	public RestValidationResult(){
		this.messages = new ArrayList<RestValidationMessage>();
	}
	
	public List<RestValidationMessage> getMessages(){
		return messages;
	}
	
	/**
	 * Add Validation Error
	 * @param msg
	 * @param property with validation error
	 */
	public void addError(TranslatableMessage msg, String property){
		this.messages.add(new RestValidationMessage(msg, RestMessageLevel.ERROR, property));
	}
	
	/**
	 * Add Validation Error
	 * @param key - i18n key
	 * @param property with validation error
	 */
	public void addError(String key, String property){
		this.messages.add(new RestValidationMessage(new TranslatableMessage(key), RestMessageLevel.ERROR, property));
	}
	
	/**
	 * Add an invalid value message for a property
	 * @param property
	 */
	public void addInvalidValueError(String property){
		this.messages.add(new RestValidationMessage(new TranslatableMessage("validate.invalidValue"), RestMessageLevel.ERROR, property));
	}
	
	/**
	 * Add a 'required' message for a property
	 * @param property
	 */
	public void addRequiredError(String property){
		this.messages.add(new RestValidationMessage(new TranslatableMessage("validate.required"), RestMessageLevel.ERROR, property));
	}
	
	/**
	 * If there are messages throw exception
	 * @throws ValidationFailedRestException
	 */
	public void ensureValid() throws ValidationFailedRestException{
		if(messages.size() > 0)
			throw new ValidationFailedRestException(this);
	}
	
}
