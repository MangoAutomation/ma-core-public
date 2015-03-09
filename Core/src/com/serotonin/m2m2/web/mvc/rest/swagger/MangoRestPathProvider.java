/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.swagger;

import org.springframework.stereotype.Component;

import com.mangofactory.swagger.paths.SwaggerPathProvider;

/**
 * @author Terry Packer
 * 
 */
@Component
public class MangoRestPathProvider extends SwaggerPathProvider {

	@Override
	protected String applicationPath() {
		return "/rest"; //getAppRoot().build().toString();
	}

	@Override
	protected String getDocumentationPath() {
		return ""; //getAppRoot().path("/rest/api-docs").build().toString();
	}

}
