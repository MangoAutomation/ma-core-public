/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller;



/**
 * 
 * Controller for Data Source View
 * 
 * Data Source Edit view
 * Data Source Table view
 * 
 * @author Terry Packer
 * 
 */
public class DataSourcePropertiesController extends BaseDataSourceController {
    
	public DataSourcePropertiesController(){
		super("/WEB-INF/jsp/dataSourceProperties.jsp", "/data_source_properties_error.shtm");
	}

}
