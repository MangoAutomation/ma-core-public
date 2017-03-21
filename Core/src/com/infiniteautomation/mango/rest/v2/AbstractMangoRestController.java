/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import javax.servlet.http.HttpServletRequest;

import com.infiniteautomation.mango.rest.v2.exception.ForbiddenAccessRestException;
import com.infiniteautomation.mango.rest.v2.exception.InvalidRQLRestException;
import com.infiniteautomation.mango.rest.v2.exception.UnauthorizedRestException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import net.jazdw.rql.parser.RQLParserException;

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
	@Deprecated //TODO Use @PreAuthorize instead
    protected User checkAdminUser(HttpServletRequest request) {
        User user = Common.getHttpUser();
        if(user == null)
            throw new UnauthorizedRestException();
        if(!user.isAdmin())
            throw new ForbiddenAccessRestException(user);
        return user;
    }
    
    /**
     * Check to see 
     * @param request
     * @return
     * @throws UnauthorizedRestException if the user is not authenticated
     * @throws ForbiddenAccessRestException if the user does not have general Data Source priveleges
     */
    @Deprecated //TODO Use @PreAuthorize instead
    protected User checkDataSourceUser(HttpServletRequest request){
    	User user = Common.getHttpUser();
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
	 * @param permissions - Comma separated list of acceptable permissions
	 * @return User that is logged in
     * @throws UnauthorizedRestException if the user is not authenticated
     * @throws ForbiddenAccessRestException if the user does not have all of the permissions
	 */
    @Deprecated //TODO Use @PreAuthorize instead
	protected User checkUser(HttpServletRequest request, String... permissions) {
		User user = Common.getHttpUser();
		if(user == null)
			throw new UnauthorizedRestException();
		
		for(String permission : permissions){
			if(!Permissions.hasPermission(user, permission))
				throw new ForbiddenAccessRestException(user);
		}
		
		return user;
	}

	/**
	 * Create an AST Node from the RQL query in the request
	 * @param request
	 * @return ASTNode or null if no query
	 * @throws InvalidRQLRestException
	 */
	protected ASTNode parseRQLtoAST(HttpServletRequest request) {
		RQLParser parser = new RQLParser();
		String query = request.getQueryString();
		if(query == null)
			return null;
		else{
			try{
				return parser.parse(query);
			}catch(RQLParserException e){
				throw new InvalidRQLRestException(query, e.getMessage());
			}
		} 
	}
	
}
