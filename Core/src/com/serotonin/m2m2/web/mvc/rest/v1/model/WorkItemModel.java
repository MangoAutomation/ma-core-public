/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

/**
 * Simple class to relay Work Item info to page
 * @author Terry Packer
 *
 */
public class WorkItemModel{
	
	private String classname;
	private String description;
	private String priority;
	
	public WorkItemModel(){ }
	
	/**
	 * @param canonincalClassname
	 * @param description
	 */
	public WorkItemModel(String canonincalClassname, String description, String priority) {
		super();
		this.classname = canonincalClassname;
		this.description = description;
		this.priority = priority;
	}
	public String getClassname() {
		return classname;
	}
	public void setClassname(String canonincalClassname) {
		this.classname = canonincalClassname;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getPriority(){
		return this.priority;
	}
	public void setPriority(String priority){
		this.priority = priority;
	}
	
	
}