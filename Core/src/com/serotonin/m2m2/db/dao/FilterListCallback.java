/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.collections.comparators.ComparatorChain;

import com.serotonin.db.MappedRowCallback;

/**
 * @author Terry Packer
 *
 */
public class FilterListCallback<T> implements MappedRowCallback<T>{

	protected List<IFilter<T>> filters;
	protected List<T> results;
	protected List<T> filteredResults;
	protected ComparatorChain chain;
	
	public FilterListCallback( List<IFilter<T>> filters, ComparatorChain chain){
		this.filters = filters;
		this.chain = chain;
	    this.results = new ArrayList<T>();
		this.filteredResults = new ArrayList<T>();
	}

	public FilterListCallback( List<IFilter<T>> filters){
		this.filters = filters;
		this.results = new ArrayList<T>();
		this.filteredResults = new ArrayList<T>();
	}
	
	
	/* (non-Javadoc)
	 * @see com.serotonin.db.MappedRowCallback#row(java.lang.Object, int)
	 */
	@Override
	public void row(T row, int rowIndex) {
		//Filter the VO
        for(IFilter<T> filter : filters){
			if(filter.filter(row)){
				//this.filteredResults.add(row);
				return;
			}
		}
		
        //Otherwise keep it
        this.results.add(row);
	}

	public List<T> getResults(){
		return this.results;
	}
	
	public void orderResults(){
		if(chain != null && chain.size() > 0)
			Collections.sort(this.results,this.chain);
	}
	
	public List<T> getFilteredResults(){
		return this.filteredResults;
	}
}
