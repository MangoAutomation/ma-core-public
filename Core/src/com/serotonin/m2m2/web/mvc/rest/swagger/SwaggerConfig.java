/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.serotonin.m2m2.Common;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.model.ApiInfo;

/**
 * @author Terry Packer
 *
 */
@EnableSwagger
@Configuration
@ComponentScan(basePackages = { "com.serotonin.m2m2.web.mvc.rest.swagger" })
public class SwaggerConfig{
   
   /**
    * Every SwaggerSpringMvcPlugin bean is picked up by the swagger-mvc framework - allowing for multiple
    * swagger groups i.e. same code base multiple swagger resource listings.
    */
   @Bean
   public SwaggerSpringMvcPlugin customImplementation(MangoRestPathProvider provider, SpringSwaggerConfig springSwaggerConfig){  

	   //Authorization TODO Add in when ready for Auth
//	    List<AuthorizationScope> scopes = new ArrayList<AuthorizationScope>();
//	    scopes.add(new AuthorizationScope("email", "Access to your email address"));
//	    scopes.add(new AuthorizationScope("pets", "Access to your pets"));
//	   List<GrantType> grantTypes = new ArrayList<GrantType>();
//
//	    ImplicitGrant implicitGrant = new ImplicitGrant(
//	      new LoginEndpoint("http://localhost:8002/oauth/dialog"), 
//	      "access_code");
//	    grantTypes.add(implicitGrant);
//
//	    AuthorizationType oauth = new OAuthBuilder().scopes(scopes).grantTypes(grantTypes).build();
	      return new SwaggerSpringMvcPlugin(springSwaggerConfig)
	      		.apiInfo(apiInfo())
	      		//.authorizationTypes(authorizationTypes)
	      		.pathProvider(provider)
	            .includePatterns(".*" + Common.envProps.getString("swagger.mangoApiVersion", "v1") + "/.*");
   }

   /**
    * Return the API Info for the Swagger UI Display
    * @return
    */
   private ApiInfo apiInfo() {
	      ApiInfo apiInfo = new ApiInfo(
	    	      "Mango Rest API",                             /* title */
	    	      "Mango Automation  " + 
	    	      "at <a href=\"http://infiniteautomation.com\">http://infiniteautomation.com</a> " + 
	    	      "we need to define an api key \"special-key\" to test the authorization filters", 
	    	      "http://infiniteautomation.com/terms/",           /* TOS URL */
	    	      "info@infiniteautomation.com",                            /* Contact */
	    	      "Apache 2.0",                                     /* license */
	    	      "http://www.apache.org/licenses/LICENSE-2.0.html" /* license URL */
	        );
	      return apiInfo;
	    }
   
   
   
}