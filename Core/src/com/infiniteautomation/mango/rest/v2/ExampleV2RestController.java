/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.vo.User;
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
public class ExampleV2RestController extends AbstractMangoRestV2Controller{
	
	@PreAuthorize("isAdmin()")
	@ApiOperation(value = "Example User Credentials test", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/admin-get/{resourceId}"}, produces = {"application/json"} )
	public ResponseEntity<Object> exampleGet(@AuthenticationPrincipal User user,
			@ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId) {
		RestProcessResult<Object> result = new RestProcessResult<>(HttpStatus.OK);

		//TODO Check to see if the resourceId is 'test' if not throw NotFoundRestException
		
		return result.createResponseEntity();
	}
	

	@PreAuthorize("hasAllPermissions('superadmin','user')")
	@ApiOperation(value = "Example User Credentials test", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/user-get/{resourceId}"}, produces = {"application/json"} )
	public ResponseEntity<Object> userGet(@ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId) {
		RestProcessResult<Object> result = new RestProcessResult<>(HttpStatus.OK);
		
		return result.createResponseEntity();
	}
	
	
}
