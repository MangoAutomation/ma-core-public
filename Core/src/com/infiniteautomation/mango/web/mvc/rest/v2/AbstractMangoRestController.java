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
     * Check to see if a User is logged in and is an admin
     * 
     * @param request
     * @param result
     * @return User that is logged in
     * @throws UnauthorizedRestException if the user is not authenticated
     * @throws ForbiddenAccessRestException if the user is not an admin
     */
    protected User checkAdminUser(HttpServletRequest request) {
        User user = Common.getUser(request);
        if(user == null)
            throw new UnauthorizedRestException();
        if(!user.isAdmin())
            throw new ForbiddenAccessRestException(user);
        return user;
    }
    
	/**
	 * Check to see if a User is logged in and has permissions
	 * 
	 * @param request
	 * @param result
	 * @return User that is logged in
     * @throws UnauthorizedRestException if the user is not authenticated
     * @throws ForbiddenAccessRestException if the user does not have all of the permissions
	 */
	protected User checkUser(HttpServletRequest request, String... permissions) {
		User user = Common.getUser(request);
		if(user == null)
			throw new UnauthorizedRestException();
		
		// TODO check permissions
		
		return user;
	}

	protected ASTNode parseRQLtoAST(HttpServletRequest request) throws UnsupportedEncodingException {
		RQLParser parser = new RQLParser();
		String query = request.getQueryString();
		if(query == null)
			return null;
        return parser.parse(query);
	}
	
}
