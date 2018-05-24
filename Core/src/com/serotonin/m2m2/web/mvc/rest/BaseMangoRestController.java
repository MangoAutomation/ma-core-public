/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.infiniteautomation.mango.rest.v2.exception.InvalidRQLRestException;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;

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

    private static final ASTNode DEFAULT_NODE = new ASTNode("limit", AbstractBasicDao.DEFAULT_LIMIT);
    private static final Pattern FORMAT_PARAMETER_PATTERN = Pattern.compile("(?:^|&)format=[\\w-]+?(?:$|&)");

    /**
     * Use static method BaseMangoRestController.parseRQLtoAST(String queryString) instead.
     *
     * Create an AST Node from the RQL query in the request
     * @param request
     * @return ASTNode
     * @throws InvalidRQLRestException
     */
    @Deprecated
    public ASTNode parseRQLtoAST(HttpServletRequest request) throws InvalidRQLRestException {
        String query = request.getQueryString();
        return parseRQLtoAST(query);
    }

    /**
     * Create an AST Node from the RQL query in the request
     * @param queryString
     * @return ASTNode
     * @throws InvalidRQLRestException
     */
    public static ASTNode parseRQLtoAST(String queryString) throws InvalidRQLRestException {
        if (queryString == null || queryString.isEmpty()) return DEFAULT_NODE;

        // remove format=x e.g. format=csv parameter
        queryString = FORMAT_PARAMETER_PATTERN.matcher(queryString).replaceFirst("");
        if (queryString.isEmpty()) return DEFAULT_NODE;

        RQLParser parser = new RQLParser();

        try {
            return parser.parse(queryString);
        } catch (RQLParserException | IllegalArgumentException e) {
            throw new InvalidRQLRestException(queryString, e.getMessage());
        }
    }

    /**
     * Append an AND Restriction to a query
     * @param query - can be null
     * @param restriction
     * @return
     */
    protected static ASTNode addAndRestriction(ASTNode query, ASTNode restriction){
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
    protected static ASTNode addOrRestriction(ASTNode query, ASTNode restriction){
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
