/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * @author Terry Packer
 *
 */
@ApiModel(value="AbstractRestModel", description="Base Data Model")
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
	 * Used to validate the model.
	 * Override as required in subclassess
	 * @param response
	 */
	@JsonIgnore
	public boolean validate(){
		return true;
	}

}