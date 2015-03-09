/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.converter;


/**
 * Container for mapping type in JSON to class
 * 
 * @author Terry Packer
 *
 */
public class PointLocatorDefinition {

	private String exportName;
	private Class<?> typeClass;
	/**
	 * @param exportName
	 * @param typeClass
	 */
	public PointLocatorDefinition(String exportName, Class<?> typeClass) {
		super();
		this.exportName = exportName;
		this.typeClass = typeClass;
	}
	public String getExportName() {
		return exportName;
	}
	public Class<?> getTypeClass() {
		return typeClass;
	}
	
	


}
