/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * @author Terry Packer
 *
 */
@ApiModel(value="AbstractRestModel", description="Base Data Model")
@JsonEntity
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
	 * 
	 * @param response
	 */
	public void validate(RestProcessResult<?> response) throws RestValidationFailedException{
		return;
	}

}