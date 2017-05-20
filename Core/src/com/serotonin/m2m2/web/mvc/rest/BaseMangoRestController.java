/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest;

import javax.servlet.http.HttpServletRequest;

import com.infiniteautomation.mango.rest.v2.exception.InvalidRQLRestException;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import net.jazdw.rql.parser.RQLParserException;

/**
 * 
 * Base of all Rest controllers to enable RQL/AST Support
 * 
 * @author Terry Packer
 */
public abstract class BaseMangoRestController {

	/**
	 * Create an AST Node from the RQL query in the request
	 * @param request
	 * @return ASTNode or null if no query
	 * @throws InvalidRQLRestException
	 */
	protected ASTNode parseRQLtoAST(HttpServletRequest request) throws InvalidRQLRestException{
		RQLParser parser = new RQLParser();
		String query = request.getQueryString();
		if(query == null)
			return null;
		else{
			try{
				return parser.parse(query);
			}catch(RQLParserException | IllegalArgumentException e){
				throw new InvalidRQLRestException(query, e.getMessage());
			}
		} 
	}
	
	/**
	 * Append an AND Restriction to a query
	 * @param query - can be null
	 * @param restriction
	 * @return
	 */
	protected ASTNode addAndRestriction(ASTNode query, ASTNode restriction){
		//Root query node
		ASTNode root = null;
		
		if(query == null){
			root = restriction;
		}else if(query.getName().equalsIgnoreCase("and")){
			root = query.addArgument(restriction);
		}else{
			root = new ASTNode("and", restriction, query);
		}
		return root;
	}
	
	/**
	 * Append an OR restriction to the query
	 * @param query - can be null
	 * @param restriction
	 * @return
	 */
	protected ASTNode addOrRestriction(ASTNode query, ASTNode restriction){
		//Root query node
		ASTNode root = null;
		
		if(query == null){
			root = restriction;
		}else if(query.getName().equalsIgnoreCase("or")){
			root = query.addArgument(restriction);
		}else{
			root = new ASTNode("or", restriction, query);
		}
		return root;
	}
}
