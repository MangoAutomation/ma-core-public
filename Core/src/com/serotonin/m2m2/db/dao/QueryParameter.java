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
	
	private String property;
	private String attribute;
	
	public QueryParameter() { }
	
	public QueryParameter(String property, String condition){
		this.property = property;
		this.attribute = condition;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getAttribute() {
		return attribute;
	}

	public void getAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	
}
