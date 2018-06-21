/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.models.dto.ApiInfo;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.serotonin.m2m2.Common;


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
                //.ignoredParameterTypes(ObjectMapper.class) //Ignore these types for Doc generation
                //.authorizationTypes(authorizationTypes)
                .pathProvider(provider)
                .includePatterns(".*" + Common.envProps.getString("swagger.mangoApiVersion", "v[12]") + "/.*");
    }

    /**
     * Return the API Info for the Swagger UI Display
     * @return
     */
    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "Mango Rest API",                             /* title */
                //Sub-title and support info,
                "Support: <a href='http://infiniteautomation.com/forum'>Forum</a>&nbsp or &nbsp <a href='http://infiniteautomation.com/wiki/doku.php?id=graphics:api:intro'>Wiki</a>",
                "http://infiniteautomation.com/terms/",           /* TOS URL */
                "info@infiniteautomation.com",                    /* Contact */
                "Apache 2.0",                                     /* license */
                "http://www.apache.org/licenses/LICENSE-2.0.html" /* license URL */
                );
        return apiInfo;
    }



}