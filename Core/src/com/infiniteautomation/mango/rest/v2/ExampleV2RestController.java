/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * 
 * @author Terry Packer
 */
@Api(value="Example Controller", description="Test for new controller type")
@RestController
@RequestMapping("/v2/example")
public class ExampleV2RestController extends AbstractMangoRestController{
	
	
	@ApiOperation(value = "Example User Credentials test", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/get/{resourceId}"}, produces = {"application/json"} )
	public ResponseEntity<Object> exampleGet(HttpServletRequest request, 
			@ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId) {
		RestProcessResult<Object> result = new RestProcessResult<>(HttpStatus.OK);

		//If not logged in then throw UnauthorizedRestException
        //If not admin then throw ForbiddenAccessRestException
		this.checkAdminUser(request);

		//TODO Check to see if the resourceId is 'test' if not throw NotFoundRestException
		
		return result.createResponseEntity();
	}
	

	
}
