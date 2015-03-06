/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author Terry Packer
 *
 */
public abstract class SuperclassModel<T> extends AbstractRestModel<SuperclassModel<T>> {

	public SuperclassModel(){
		super(null);
	}
	/**
	 * @param data
	 */
	public SuperclassModel(SuperclassModel<T> data) {
		super(data);
	}

	@JsonGetter("type")
	public abstract String getType();

	
	@JsonSetter("type")
	public void setType(String typeName) { }

	
	
	
}
