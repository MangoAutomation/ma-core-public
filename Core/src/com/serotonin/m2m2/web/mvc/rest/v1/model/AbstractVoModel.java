/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractVoModel<T extends AbstractVO<T>> extends AbstractRestModel<AbstractVO<T>>{
	
	
	/**
	 * @param data
	 */
	public AbstractVoModel(AbstractVO<T> data) {
		super(data);
	}

	@JsonGetter("xid")
	public String getXid(){
		return this.data.getXid();
	}
	@JsonSetter("xid")
	public void setXid(String xid){
		this.data.setXid(xid);
	}
	
	@JsonGetter("name")
	public String getName(){
		return this.data.getName();
	}
	@JsonSetter("name")
	public void setName(String name){
		this.data.setName(name);
	}
	

	
	/**
	 * Validate the model, adding failure messages to the response
	 * @param response
	 */
	@Override
	public void validate(ProcessResult response){
		this.data.validate(response);
	}

}
