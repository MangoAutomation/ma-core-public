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
public class UnauthorizedController extends ParameterizableViewController{
	public UnauthorizedController(){
		super();
		setViewName("/WEB-INF/jsp/unauthorized.jsp");
	}
}
