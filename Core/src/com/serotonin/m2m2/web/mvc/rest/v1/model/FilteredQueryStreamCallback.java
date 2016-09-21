/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Terry Packer
 *
 */
public class FilteredQueryStreamCallback<T> extends QueryStreamCallback<T>{

	private final Log LOG = LogFactory.getLog(FilteredQueryStreamCallback.class);
	
	//Count of items filtered out by the filter() method
	protected long filtered;
	//Our virtual result set counter
	protected int offsetCount;
	protected int writtenCount;
	
	//Query Values
	protected Integer limit = Integer.MAX_VALUE;
	protected Integer offset = Integer.MIN_VALUE;
	
	/* (non-Javadoc)
	 * @see com.serotonin.db.MappedRowCallback#row(java.lang.Object, int)
	 */
	@Override
	public void row(T vo, int resultSetIndex) {
		try {
			if(filter(vo))
				this.filtered++;
			else{
				if((this.writtenCount < this.limit) && (this.offsetCount >= this.offset)){
					this.write(vo);
					this.writtenCount++;
				}
				this.offsetCount++;
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		
	}
	
	public void setLimit(Integer limit){
		this.limit = limit;
	}
	public void setOffset(Integer offset){
		this.offset = offset;
	}
	
	/**
	 * Allow filtering within stream outside of query.
	 * Return true to filter out this object
	 * @param vo
	 * @return
	 */
	protected boolean filter(T vo){
		return false;
	}
	
	public long getFilteredCount(){
		return this.filtered;
	}
	
	public void setFilteredCount(long count){
		this.filtered = count;
	}
	
}
