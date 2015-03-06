/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVEntity;

/**
 * @author Terry Packer
 *
 */
@CSVEntity
@JsonEntity
@JsonPropertyOrder({"enabled"})
public abstract class AbstractActionVoModel<T extends AbstractActionVO<T>> extends AbstractVoModel<T>{

	@JsonIgnore
	protected T data;
	/**
	 * @param data
	 */
	public AbstractActionVoModel(AbstractActionVO<T> data) {
		super(data);
		this.data = (T)data;
	}

	@CSVColumnGetter(order=2, header="enabled")
	@JsonGetter(value="enabled")
	public boolean isEnabled(){
		return this.data.isEnabled();
	}
	@CSVColumnSetter(order=2, header="enabled")
	@JsonSetter(value="enabled")
	public void setEnabled(boolean enabled){
		this.data.setEnabled(enabled);
	}

	public T getData(){
		return this.data;
	}
}
