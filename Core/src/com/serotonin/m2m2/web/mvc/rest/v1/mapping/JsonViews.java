/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

/**
 * Definition of various JSON Views
 * 
 * 
 * @author Terry Packer
 *
 */
public class JsonViews {
	
	//Views to return sub-sets of data for each model
	public static class Test{} //View for returning data that is required in tests but nowhere else
	public static class Validation {} //View for returning Validation messages with the data
}
