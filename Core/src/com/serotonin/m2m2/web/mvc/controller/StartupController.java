/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.controller;

import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Controller to show Startup Page
 * 
 * 
 * @author Terry Packer
 *
 */
public class StartupController extends ParameterizableViewController {
	public StartupController(){
		super();
		setViewName("/WEB-INF/jsp/starting.jsp");
	}     
}
