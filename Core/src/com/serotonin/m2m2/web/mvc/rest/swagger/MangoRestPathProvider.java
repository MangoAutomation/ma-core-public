/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.swagger;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.mangofactory.swagger.paths.SwaggerPathProvider;

/**
 * @author Terry Packer
 * 
 */
@Component
public class MangoRestPathProvider extends SwaggerPathProvider {

	private final static String prefix = "/rest";
	
	@Autowired
	private ServletContext servletContext;

	@Override
	protected String applicationPath() {
		return getAppRoot().build().toString();
	}

	@Override
	protected String getDocumentationPath() {
		return getAppRoot().path("/rest/api-docs").build().toString();
	}

	private UriComponentsBuilder getAppRoot() {
		return UriComponentsBuilder.fromHttpUrl("http://localhost:8080").path(
				servletContext.getContextPath());
	}
/* (non-Javadoc)
 * @see com.mangofactory.swagger.paths.SwaggerPathProvider#setApiResourcePrefix(java.lang.String)
 */
	@Override
	public void setApiResourcePrefix(String apiResourcePrefix) {
		super.setApiResourcePrefix(apiResourcePrefix);
	}
	
	@Override
	public String getApiResourcePrefix(){
		return prefix;
	}

	/* (non-Javadoc)
	 * @see com.mangofactory.swagger.paths.SwaggerPathProvider#getOperationPath(java.lang.String)
	 */
	@Override
	public String getOperationPath(String operationPath) {
		return getApiResourcePrefix() + operationPath;
	}

}
