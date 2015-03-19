/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

/**
 * @author Terry Packer
 *
 */
public class QueryParameter {
	
	private String attribute;
	private String condition;
	
	public QueryParameter() { }
	
	public QueryParameter(String attribute, String condition){
		this.attribute = attribute;
		this.condition = condition;
	}

	public String getAttribute() {
		return attribute;
	}

	public void getAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	
	
}
