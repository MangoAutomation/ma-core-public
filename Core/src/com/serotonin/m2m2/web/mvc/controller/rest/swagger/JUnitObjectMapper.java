/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller.rest.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Terry Packer
 *
 */
public class JUnitObjectMapper extends ObjectMapper{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JUnitObjectMapper(){
		JUnitModule junitModule = new JUnitModule();
		registerModule(junitModule);
	}
	
	
}
