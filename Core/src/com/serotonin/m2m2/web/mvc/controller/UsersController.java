/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * @author Terry Packer
 *
 */
@Controller
public class UsersController extends ParameterizableViewController{
	public UsersController(){
		super();
		setViewName("/WEB-INF/jsp/users.jsp");
	}

}
