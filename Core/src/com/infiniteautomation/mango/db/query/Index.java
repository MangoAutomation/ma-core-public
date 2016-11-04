/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.List;

/**
 * Class to describe table indexes
 * 
 * @author Terry Packer
 *
 */
public class Index {
	
	private String name;
	private String tablePrefix;
	private List<QueryAttribute> columns;
	private String type; //ASC,DESC,ETC for later use
	
	/**
	 * @param name
	 * @param columns
	 * @param type
	 */
	public Index(String name, String tablePrefix, List<QueryAttribute> columns, String type) {
		super();
		this.name = name;
		this.tablePrefix = tablePrefix;
		this.columns = columns;
		this.type = type;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the tablePrefix
	 */
	public String getTablePrefix() {
		return tablePrefix;
	}
	/**
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
	/**
	 * @return the columns
	 */
	public List<QueryAttribute> getColumns() {
		return columns;
	}
	/**
	 * @param columns the columns to set
	 */
	public void setColumns(List<QueryAttribute> columns) {
		this.columns = columns;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

}
