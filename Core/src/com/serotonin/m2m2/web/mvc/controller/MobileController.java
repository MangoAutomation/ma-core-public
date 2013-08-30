/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * @author Terry Packer
 *
 */
public class MobileController extends ParameterizableViewController {
    private String errorViewName;

    public void setErrorViewName(String errorViewName) {
        this.errorViewName = errorViewName;
    }

}
