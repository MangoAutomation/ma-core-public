/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
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

	@JsonGetter(value="id")
	public int getId(){
		return this.data.getId();
	}
	@JsonSetter(value="id")
	public void setId(int id){
		this.data.setId(id);
	}
	
	@JsonGetter(value="xid")
	public String getXid(){
		return this.data.getXid();
	}
	@JsonSetter(value="xid")
	public void setXid(String xid){
		this.data.setXid(xid);
	}
	
	@JsonGetter(value="name")
	public String getName(){
		return this.data.getName();
	}
	@JsonSetter(value="name")
	public void setName(String name){
		this.data.setName(name);
	}
	
}
