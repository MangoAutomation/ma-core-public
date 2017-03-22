/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;
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
	public ResponseEntity<Object> userGet(@AuthenticationPrincipal User user, 
			@ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId) {
		RestProcessResult<Object> result = new RestProcessResult<>(HttpStatus.OK);
		
		return result.createResponseEntity();
	}
	
	@PreAuthorize("hasAllPermissions('user')")
	@ApiOperation(value = "Example Permission Exception Response", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/permissions-exception"}, produces = {"application/json"} )
	public ResponseEntity<Object> alwaysFails(@AuthenticationPrincipal User user) {
		throw new PermissionException("I always fail.", user);
	}
	
	@PreAuthorize("isAdmin()")
	@ApiOperation(value = "Expire the session of the current user", notes = "must be admin")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/expire-session"}, produces = {"application/json"} )
	public ResponseEntity<Object> expireSessions(@AuthenticationPrincipal User user){
		RestProcessResult<Object> result = new RestProcessResult<>(HttpStatus.OK);
		
		List<SessionInformation> infos = MangoSecurityConfiguration.sessionRegistry().getAllSessions(user, false);
		for(SessionInformation info : infos)
			info.expireNow();
		return result.createResponseEntity();
	}
}
