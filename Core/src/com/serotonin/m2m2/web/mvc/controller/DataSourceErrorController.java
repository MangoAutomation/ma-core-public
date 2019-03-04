/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import org.springframework.stereotype.Controller;

/**
 * Controller to handle Data Source Page Errors
 * 
 * @author Terry Packer
 *
 */
@Controller
public class DataSourceErrorController extends ErrorController {

	public DataSourceErrorController(){
		super();
		setViewName("/WEB-INF/jsp/error/dataSourceError.jsp");
	}
}
