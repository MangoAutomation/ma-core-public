/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.swagger;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.classmate.TypeResolver;
import com.google.common.net.HttpHeaders;
import com.serotonin.m2m2.Common;

import io.swagger.models.auth.In;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRule;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.AbstractPathProvider;
import springfox.documentation.spring.web.paths.Paths;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


/**
 * Simple Swagger 2 Configuration
 * 
 * TODO: Add Token Authentication if we want to expose api-docs endpoint to public
 * 
 * @author Terry Packer
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
	
	@Autowired
	private TypeResolver typeResolver;
	
	@Value("${springfox.documentation.swagger.v2.path}")
	private String swagger2Endpoint;
	
	@Bean
	public Docket describe(){
		
		Docket docket = new Docket(DocumentationType.SWAGGER_2)
		        .ignoredParameterTypes(AuthenticationPrincipal.class)
		        .select()
		          .apis(RequestHandlerSelectors.any())
		          .paths(PathSelectors.regex("/" + Common.envProps.getString("swagger.mangoApiVersion", "v1") + "/.*"))
		          .build()
		          .securitySchemes(Arrays.asList(new ApiKey("Mango Token", HttpHeaders.AUTHORIZATION, In.HEADER.name())))
		          .pathProvider(new BasePathAwareRelativePathProvider("/rest"))
		        .genericModelSubstitutes(ResponseEntity.class);
		
		docket.alternateTypeRules(
		            new AlternateTypeRule(typeResolver.resolve(DeferredResult.class,
		                    typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
		                typeResolver.resolve(WildcardType.class)))
		        .useDefaultResponseMessages(false);

		docket.apiInfo(new ApiInfoBuilder()
	              .title("Mango REST API")
	              .description("Support: <a href='http://infiniteautomation.com/forum'>Forum</a> or <a href='http://infiniteautomation.com/wiki/doku.php?id=graphics:api:intro'>Wiki</a>")
	              .version("2.0")
	              .termsOfServiceUrl("https://infiniteautomation.com/terms/")
	              .contact(new Contact("IAS", "https://infiniteautomation.com", "info@infiniteautomation.com"))
	              .license("Apache 2.0")
	              .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html")
	              .build());
		return docket;
	}
	
	class BasePathAwareRelativePathProvider extends AbstractPathProvider {
        private String basePath;

        public BasePathAwareRelativePathProvider(String basePath) {
            this.basePath = basePath;
        }

        @Override
        protected String applicationPath() {
            return basePath;
        }

        @Override
        protected String getDocumentationPath() {
            return basePath;
        }

        @Override
        public String getOperationPath(String operationPath) {
            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath("/");
            return Paths.removeAdjacentForwardSlashes(
                    uriComponentsBuilder.path(operationPath.replaceFirst(basePath, "")).build().toString());
        }
    }
}