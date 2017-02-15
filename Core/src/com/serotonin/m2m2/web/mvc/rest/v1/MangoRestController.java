/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.message.ResourceCreatedMessage;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestMessage;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;

/**
 * @author Terry Packer
 * 
 */
public abstract class MangoRestController{

	//private static final Log LOG = LogFactory.getLog(MangoRestController.class);
	
	/**
	 * Check to see if a User is logged in
	 * 
	 * TODO could potentially use the @SessionAttributes({"user"}) annotation for this
	 * 
	 * @param request
	 * @param result
	 * @return User that is logged in, null if none are
	 */
	protected User checkUser(HttpServletRequest request, @SuppressWarnings("rawtypes") RestProcessResult  result) {
		User user = Common.getUser(request);
		if(user == null){
			result.addRestMessage(HttpStatus.UNAUTHORIZED, new TranslatableMessage("common.default", "User not logged in"));
		}
		
		return user;
	}
	
	/**
	 * Helper to easily change stock messages
	 * @return
	 */
	public RestMessage getUnauthorizedMessage(){
		return new RestMessage(HttpStatus.UNAUTHORIZED,new TranslatableMessage("common.default", "Unauthorized access"));
	}
	
	/**
	 * Helper to easily change stock messages for trying to create something that exists
	 * @return
	 */
	public RestMessage getAlreadyExistsMessage(){
		return new RestMessage(HttpStatus.CONFLICT, new TranslatableMessage("common.default", "Item already exists"));
	}
	
	/**
	 * Helper to easily change stock messages for failing to find something
	 * @return
	 */
	public RestMessage getDoesNotExistMessage(){
		return new RestMessage(HttpStatus.NOT_FOUND, new TranslatableMessage("common.default", "Item does not exist"));
	}
	/**
	 * Helper to easily change stock messages for successful operations
	 * @return
	 */
	public RestMessage getSuccessMessage(){
		return new RestMessage(HttpStatus.OK, new TranslatableMessage("common.default", "Success"));
	}
	
	/**
	 * Helper to create Validation Failed Rest Messages
	 * @return
	 */
	public RestMessage getValidationFailedError(){
		return new RestMessage(HttpStatus.UNPROCESSABLE_ENTITY, new TranslatableMessage("common.default", "Validation error"));
	}
	
	public RestMessage getResourceCreatedMessage(URI location){
		return new ResourceCreatedMessage(HttpStatus.CREATED, new TranslatableMessage("common.default", "Created"), location);
	}
	
	public RestMessage getResourceUpdatedMessage(URI location){
		return new ResourceCreatedMessage(HttpStatus.OK, new TranslatableMessage("common.default", "Updated"), location);
	}

	public RestMessage getInternalServerErrorMessage(String content){
		return new RestMessage(HttpStatus.INTERNAL_SERVER_ERROR, new TranslatableMessage("common.default", content));
	}
	
	protected ASTNode parseRQLtoAST(HttpServletRequest request) throws UnsupportedEncodingException {
		RQLParser parser = new RQLParser();
		String query = request.getQueryString();
		if(query == null)
			return null;
        return parser.parse(query);
	}
}
