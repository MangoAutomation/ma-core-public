/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.db.dao.SortOption;

/**
 * @author Terry Packer
 *
 */
public class QueryDefinition {
	
	protected Map<String,String> query;
	protected List<SortOption> sort;
	protected boolean or; //Use OR or AND in queries
	/**
	 * @param query
	 * @param sort
	 * @param or
	 */
	public QueryDefinition(Map<String, String> query, List<SortOption> sort,
			boolean or) {
		super();
		this.query = query;
		this.sort = sort;
		this.or = or;
	}
	public Map<String, String> getQuery() {
		return query;
	}
	public void setQuery(Map<String, String> query) {
		this.query = query;
	}
	public List<SortOption> getSort() {
		return sort;
	}
	public void setSort(List<SortOption> sort) {
		this.sort = sort;
	}
	public boolean isOr() {
		return or;
	}
	public void setOr(boolean or) {
		this.or = or;
	}
	
	

}
