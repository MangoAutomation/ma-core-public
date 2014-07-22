/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller.rest.swagger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.wordnik.swagger.model.ApiInfo;

/**
 * @author Terry Packer
 *
 */
//@Configuration
//@EnableWebMvc
//@EnableSwagger
//@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.controller.rest"}) 
public class SwaggerConfig {

   private SpringSwaggerConfig springSwaggerConfig;

   /**
    * Required to autowire SpringSwaggerConfig
    */
   @Autowired
   public void setSpringSwaggerConfig(SpringSwaggerConfig springSwaggerConfig) {
      this.springSwaggerConfig = springSwaggerConfig;
   }

   /**
    * Every SwaggerSpringMvcPlugin bean is picked up by the swagger-mvc framework - allowing for multiple
    * swagger groups i.e. same code base multiple swagger resource listings.
    */
   @Bean
   public SwaggerSpringMvcPlugin customImplementation(){
      return new SwaggerSpringMvcPlugin(this.springSwaggerConfig)
      		.apiInfo(apiInfo())
            .includePatterns(".*v1/.*");
   }

   private ApiInfo apiInfo() {
	      ApiInfo apiInfo = new ApiInfo(
	              "Mango API Title",
	              "Mango API Description",
	              "Mango API terms of service",
	              "Mango API Contact Email",
	              "Mango API Licence Type",
	              "Mango API License URL"
	        );
	      return apiInfo;
	    }
   
   
   
}