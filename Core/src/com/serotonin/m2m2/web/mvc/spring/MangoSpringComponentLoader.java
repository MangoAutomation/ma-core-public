/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.swagger.SwaggerConfig;

/**
 * @author Terry Packer
 *
 */
public class MangoSpringComponentLoader {

	/**
	 * Any initial setup for Spring Web Application Context that is required can 
	 * be performed here without having to worry about Autowiring.
	 * 
	 * This is useful for using Mango env properties to control
	 * what Spring Configurations to load.
	 * 
	 * 
	 * @param webApplicationContext
	 */
	public static void setupSpring(WebApplicationContext webApplicationContext) {
		
		AnnotationConfigWebApplicationContext annotationContext = (AnnotationConfigWebApplicationContext)webApplicationContext;

		boolean enableRest = Common.envProps.getBoolean("rest.enabled", false);
		boolean enableSwagger = Common.envProps.getBoolean("swagger.enabled", false);
		
		if(enableRest){
			annotationContext.register(MangoRestSpringConfiguration.class);
		}
		
		if(enableSwagger&&enableRest){
			annotationContext.register(SwaggerConfig.class);
		}
		
		annotationContext.refresh();		
		
	}

}
