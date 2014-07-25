/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractRestModel<T> {
	
	@JsonIgnore
	protected T data;
	
	public AbstractRestModel(T data){
		this.data = data;
	}
	
	/**
	 * Get the data for the model
	 * @return T
	 */
	public T getData(){
		return data;
	}
	
	/**
	 * Validate the model, adding failure messages to the response
	 * @param response
	 */
	public abstract void validate(ProcessResult response);
	
	
}
