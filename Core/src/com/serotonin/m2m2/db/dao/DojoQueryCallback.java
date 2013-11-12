/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.db.MappedRowCallback;

/**
 * Class to help manage query callbacks
 * 
 *  allowing to keep results and a count of them
 * 
 * @author Terry Packer
 *
 */
public class DojoQueryCallback<T> implements MappedRowCallback<T>{

	private boolean keepResults;
	private List<T> results;
	private int resultCount;
	
	public DojoQueryCallback(boolean keepResults){
		this.keepResults = keepResults;
		this.results = new ArrayList<T>();
	}
	
	
	/* (non-Javadoc)
	 * @see com.serotonin.db.MappedRowCallback#row(java.lang.Object, int)
	 */
	@Override
	public void row(T row, int rowNum) {
		resultCount++; //Increment our count
		if(keepResults) //Keep if we are supposed to
			this.results.add(row);
	}

	public boolean isKeepResults(){
		return this.keepResults;
	}
	public List<T> getResults(){
		return this.results;
	}
	public int getResultCount(){
		return this.resultCount;
	}
}
