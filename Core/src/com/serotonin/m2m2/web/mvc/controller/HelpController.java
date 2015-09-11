/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * @author Terry Packer
 *
 */
public class HelpController extends ParameterizableViewController{
	
	public HelpController(){
		super();
		this.setViewName("/WEB-INF/jsp/help.jsp");
	}

}
