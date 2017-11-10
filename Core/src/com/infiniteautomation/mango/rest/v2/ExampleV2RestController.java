/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import com.infiniteautomation.mango.rest.v2.exception.GenericRestException;
import com.serotonin.m2m2.LicenseViolatedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
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
		return new ResponseEntity<Object>(HttpStatus.OK);
	}
	

	@PreAuthorize("hasAllPermissions('user')")
	@ApiOperation(value = "Example User Credentials test", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/user-get/{resourceId}"}, produces = {"application/json"} )
	public ResponseEntity<Object> userGet(@AuthenticationPrincipal User user, 
			@ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId) {
		return new ResponseEntity<Object>(HttpStatus.OK);
	}
	
	@PreAuthorize("hasAllPermissions('user')")
	@ApiOperation(value = "Example Permission Exception Response", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/permissions-exception"}, produces = {"application/json"} )
	public ResponseEntity<Object> alwaysFails(@AuthenticationPrincipal User user) {
		throw new PermissionException(new TranslatableMessage("common.default", "I always fail."), user);
	}
	
	@PreAuthorize("hasAllPermissions('user')")
	@ApiOperation(value = "Example Access Denied Exception Response", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/access-denied-exception"}, produces = {"application/json"} )
	public ResponseEntity<Object> accessDenied(@AuthenticationPrincipal User user) {
		throw new AccessDeniedException("I don't have access.");
	}
	
	@PreAuthorize("hasAllPermissions('user')")
	@ApiOperation(value = "Example Generic Rest Exception Response", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/generic-exception"}, produces = {"application/json"} )
	public ResponseEntity<Object> genericFailure(@AuthenticationPrincipal User user) {
		throw new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@PreAuthorize("hasAllPermissions('user')")
	@ApiOperation(value = "Example Runtime Exception Response", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/runtime-exception"}, produces = {"application/json"} )
	public ResponseEntity<Object> runtimeFailure(@AuthenticationPrincipal User user) {
		throw new RuntimeException("I'm a runtime Exception");
	}
	
	@PreAuthorize("hasAllPermissions('user')")
	@ApiOperation(value = "Example IOException Response", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/io-exception"}, produces = {"application/json"} )
	public ResponseEntity<Object> ioFailure(@AuthenticationPrincipal User user) throws IOException{
		throw new IOException("I'm an Exception");
	}

	@PreAuthorize("hasAllPermissions('user')")
	@ApiOperation(value = "Example LicenseViolationException Response", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/license-violation"}, produces = {"application/json"} )
	public ResponseEntity<Object> licenseViolation(@AuthenticationPrincipal User user) throws IOException{
		throw new LicenseViolatedException(new TranslatableMessage("common.default", "Test Violiation"));
	}
	
	@PreAuthorize("isAdmin()")
	@ApiOperation(value = "Expire the session of the current user", notes = "must be admin")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/expire-session"}, produces = {"application/json"} )
	public ResponseEntity<Object> expireSessions(@AuthenticationPrincipal User user){
		List<SessionInformation> infos = MangoSecurityConfiguration.sessionRegistry().getAllSessions(user, false);
		for(SessionInformation info : infos)
			info.expireNow();
		return new ResponseEntity<Object>(HttpStatus.OK);
	}
	
	@PreAuthorize("isAdmin()")
	@ApiOperation(value = "Example Path matching", notes = "")
	@ApiResponses({
		@ApiResponse(code = 401, message = "Unauthorized user access", response=ResponseEntity.class),
	})
	@RequestMapping( method = {RequestMethod.GET}, value = {"/{resourceId}/**"}, produces = {"application/json"} )
	public ResponseEntity<String> matchPath(@AuthenticationPrincipal User user,
			@ApiParam(value="Resource id", required=true, allowMultiple=false) @PathVariable String resourceId, 
			HttpServletRequest request) {
		
	    String path = (String) request.getAttribute(
	            HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
	    String bestMatchPattern = (String ) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

	    AntPathMatcher apm = new AntPathMatcher();
	    String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);
		
		return new ResponseEntity<String>(finalPath, HttpStatus.OK);
	}
	
}
