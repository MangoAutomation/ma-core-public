/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.swagger;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
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
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.paths.AbstractPathProvider;
import springfox.documentation.spring.web.paths.Paths;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


/**
 * Simple Swagger 2 Configuration
 * 
 * Note that to use the Swagger 2 UI with tokens:
 * 
 *  1.  the Value: field should be Bearar [space] [token]
 *  2.  the swagger.apidocs.protected=false must be set
 * @author Terry Packer
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
	
    private final String SECURITY_TOKEN_REFERENCE = "Mango Token";
    
	@Autowired
	private TypeResolver typeResolver;
	
	@Value("${springfox.documentation.swagger.v2.path:/swagger/v2/api-docs}")
	private String swagger2Endpoint;
	
	@Bean
	public Docket describe(){
		
		Docket docket = new Docket(DocumentationType.SWAGGER_2)
		        .ignoredParameterTypes(AuthenticationPrincipal.class)
		        .select()
		          .apis(RequestHandlerSelectors.any())
		          .paths(PathSelectors.regex("/" + Common.envProps.getString("swagger.mangoApiVersion", "v[12]") + "/.*"))
		          .build()
		          .securitySchemes(Arrays.asList(new ApiKey(SECURITY_TOKEN_REFERENCE, HttpHeaders.AUTHORIZATION, In.HEADER.name())))
		          .securityContexts(Arrays.asList(securityContext()))
		          .pathProvider(new BasePathAwareRelativePathProvider("/rest"))
		        .genericModelSubstitutes(ResponseEntity.class);
		
		docket.alternateTypeRules(
		            new AlternateTypeRule(typeResolver.resolve(DeferredResult.class,
		                    typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
		                typeResolver.resolve(WildcardType.class)),
		            //Rule to allow Multipart requests to show up as single file input
		            new AlternateTypeRule(typeResolver.resolve(MultipartHttpServletRequest.class), typeResolver.resolve(MultipartFile.class)))
		    
		        .useDefaultResponseMessages(false);

		docket.apiInfo(new ApiInfoBuilder()
	              .title("Mango REST API")
	              .description("Support: <a href='http://infiniteautomation.com/forum'>Forum</a> or <a href='https://help-infinite-automation.squarespace.com/explore-the-api/'>Help</a>")
	              .version("2.0")
	              .termsOfServiceUrl("https://infiniteautomation.com/terms/")
	              .contact(new Contact("IAS", "https://infiniteautomation.com", "support@infiniteautomation.com"))
	              .license("Apache 2.0")
	              .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html")
	              .build());
		return docket;
	}
	
	/**
	 * Setup the security context to allow Tokens to test the API
	 * @return
	 */
    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth())
                .forPaths(PathSelectors.any()).build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope[] authScopes = new AuthorizationScope[0];
        SecurityReference securityReference = SecurityReference.builder()
                .reference(SECURITY_TOKEN_REFERENCE)
                .scopes(authScopes)
                .build();
        return Arrays.asList(securityReference);
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