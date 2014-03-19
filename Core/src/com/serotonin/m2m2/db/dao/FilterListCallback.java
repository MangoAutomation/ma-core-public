/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;

import org.apache.commons.collections.comparators.ComparatorChain;

import com.serotonin.db.MappedRowCallback;

/**
 * @author Terry Packer
 *
 */
public class FilterListCallback<T> {

	protected List<IFilter<T>> filters;

	protected ComparatorChain chain;
	protected MappedRowCallback<T> onKeepCallback;
	protected MappedRowCallback<T> onFilterCallback;
	
	
	public FilterListCallback(List<IFilter<T>> filters){
		this.filters = filters;
	}
	
	public FilterListCallback( List<IFilter<T>> filters, ComparatorChain chain){
		this.filters = filters;
		this.chain = chain;
	}



	public FilterListCallback( List<IFilter<T>> filters, MappedRowCallback<T> onKeepCallback, MappedRowCallback<T> onFilterCallback){
		this.filters = filters;
		this.onKeepCallback = onKeepCallback;
		this.onFilterCallback = onFilterCallback;
	}
	

	public boolean filterRow(T row, int rowIndex) {
		//Filter the VO
        for(IFilter<T> filter : filters){
			if(filter.filter(row)){
				if(this.onFilterCallback != null)
					this.onFilterCallback.row(row,rowIndex);
				return true;
			}
		}

        //Otherwise keep it
        if(onKeepCallback != null)
        	this.onKeepCallback.row(row,rowIndex);
        return false;
	}

}
