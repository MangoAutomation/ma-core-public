/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.List;

import com.serotonin.json.spi.JsonProperty;

/**
 * Class to send results from POJO queries
 * 
 * @author Terry Packer
 *
 */
public class PageQueryResultModel<T> {

	@JsonProperty
	private List<T> items;
	@JsonProperty
	private long total;
	
	public PageQueryResultModel(){ }
	
	public PageQueryResultModel(List<T> items, long total){
		this.items = items;
		this.total = total;
	}
	
	public void setItems(List<T> items){
		this.items = items;
	}
	
	public List<T> getItems(){
		return this.items;
	}
	
	public void setTotal(long total){
		this.total = total;
	}
	public long getTotal(){
		return this.total;
	}
}
