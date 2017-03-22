/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import javax.servlet.http.HttpServletRequest;

import com.infiniteautomation.mango.rest.v2.exception.InvalidRQLRestException;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import net.jazdw.rql.parser.RQLParserException;

/**
 * Base Rest Controller for V2 of the REST api
 * 
 * @author Terry Packer
 */
public class AbstractMangoRestV2Controller {

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
