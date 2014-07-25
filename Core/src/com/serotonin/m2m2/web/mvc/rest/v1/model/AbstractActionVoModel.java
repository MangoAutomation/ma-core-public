/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.vo.AbstractActionVO;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractActionVoModel<T extends AbstractActionVO<T>> extends AbstractVoModel<T>{

	private AbstractActionVO<T> vo;
	/**
	 * @param data
	 */
	public AbstractActionVoModel(AbstractActionVO<T> data) {
		super(data);
		this.vo = data;
	}

	@JsonGetter(value="enabled")
	public boolean isEnabled(){
		return this.vo.isEnabled();
	}
	@JsonSetter(value="enabled")
	public void setEnabled(boolean enabled){
		this.vo.setEnabled(enabled);
	}

}
