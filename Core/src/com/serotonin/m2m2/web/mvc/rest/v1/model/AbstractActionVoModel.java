/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.vo.AbstractActionVO;

/**
 * @author Terry Packer
 *
 */
@JsonPropertyOrder({"enabled"})
public abstract class AbstractActionVoModel<T extends AbstractActionVO<T>> extends AbstractVoModel<T>{

	protected AbstractActionVO<T> data;
	/**
	 * @param data
	 */
	public AbstractActionVoModel(AbstractActionVO<T> data) {
		super(data);
		this.data = data;
	}

	@JsonGetter(value="enabled")
	public boolean isEnabled(){
		return this.data.isEnabled();
	}
	@JsonSetter(value="enabled")
	public void setEnabled(boolean enabled){
		this.data.setEnabled(enabled);
	}

}
