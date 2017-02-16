/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import com.infiniteautomation.mango.web.mvc.rest.v2.exception.ForbiddenAccessRestException;
import com.infiniteautomation.mango.web.mvc.rest.v2.exception.UnauthorizedRestException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;

/**
 * Base Rest Controller for V2 of the REST api
 * 
 * @author Terry Packer
 */
public class AbstractMangoRestController {

	/**
	 * Check to see if a User is logged in
	 * 
	 * @param request
	 * @param result
	 * @return User that is logged in
	 * @throws UnauthorizedRestException  
	 */
	protected User checkUser(HttpServletRequest request) throws UnauthorizedRestException {
		User user = Common.getUser(request);
		if(user == null)
			throw new UnauthorizedRestException();
		
		return user;
	}
	
	/**
	 * Ensure the user is admin, if not throw Exception
	 * 
	 * @param user
	 * @throws ForbiddenAccessRestException
	 */
	protected void ensureAdmin(User user) throws ForbiddenAccessRestException{
		if(!user.isAdmin())
			throw new ForbiddenAccessRestException(user);
	}
	
	protected ASTNode parseRQLtoAST(HttpServletRequest request) throws UnsupportedEncodingException {
		RQLParser parser = new RQLParser();
		String query = request.getQueryString();
		if(query == null)
			return null;
        return parser.parse(query);
	}
	
}
