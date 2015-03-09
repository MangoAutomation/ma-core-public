/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * I doubt we need this class around still
 * 
 * @author Terry Packer
 *
 */
@Controller
public class MobileController extends ParameterizableViewController {
	
	public MobileController(){
		super();
		setViewName("/WEB-INF/jsp/mobile/home.jsp");
		setErrorViewName("/data_source_error.shtm");
	}
	
    private String errorViewName;

    public void setErrorViewName(String errorViewName) {
        this.errorViewName = errorViewName;
    }
    public String getErrorViewName(){
    	return this.errorViewName;
    }
    

}
