/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dojo;

/**
 * @author Terry Packer
 *
 * Helper to use the Memory Store 
 *
 */
public class DojoMemoryStoreListItem {

	private String name;
	private int id;
	
	public DojoMemoryStoreListItem(String name, int id){
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}
	public int getId() {
		return id;
	}
	
}
