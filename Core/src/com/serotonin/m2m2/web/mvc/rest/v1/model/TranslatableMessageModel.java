/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.List;

/**
 * @author Terry Packer
 *
 */
public class TranslatableMessageModel {
	
	private String key;
	private List<String> args;
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public List<String> getArgs() {
		return args;
	}
	public void setArgs(List<String> args) {
		this.args = args;
	}
	
	
	

}
