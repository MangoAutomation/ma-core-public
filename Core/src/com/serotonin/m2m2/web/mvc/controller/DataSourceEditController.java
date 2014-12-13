/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import org.springframework.stereotype.Controller;

/**
 * Controller for Editing a Data Source
 * @author Terry Packer
 *
 */
@Controller
public class DataSourceEditController extends BaseDataSourceController {
	public DataSourceEditController(){
		super("/WEB-INF/jsp/dataSourceEdit.jsp", "/data_source_properties_error.shtm");
	}
}
