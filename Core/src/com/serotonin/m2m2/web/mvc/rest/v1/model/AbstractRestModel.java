/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
	 * Used to validate the model.
	 * Override as required in subclassess
	 * @param response
	 */
	@JsonIgnore
	public boolean validate(){
		return true;
	}

}