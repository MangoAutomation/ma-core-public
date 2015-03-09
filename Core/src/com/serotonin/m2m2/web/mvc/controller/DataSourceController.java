/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * @author Terry Packer
 *
 */
@Deprecated
public class DataSourceController  extends ParameterizableViewController {
    private String errorViewName;

    public void setErrorViewName(String errorViewName) {
        this.errorViewName = errorViewName;
    }

    
    //TODO Fix this up to work with the new format
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        Map<String, Object> model = new HashMap<String, Object>();
        
        //Would setup some error handling if any exceptions are thrown
        
        
        return new ModelAndView(getViewName(), model);
    }
}
