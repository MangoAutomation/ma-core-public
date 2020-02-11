/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.util.regex.Pattern;

import com.infiniteautomation.mango.util.exception.InvalidRQLException;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;

import net.jazdw.rql.converter.Converter;
import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import net.jazdw.rql.parser.RQLParserException;

/**
 * Common place for useful RQL tools
 * @author Terry Packer
 */
public class RQLUtils {

    private static final ASTNode DEFAULT_NODE = new ASTNode("limit", AbstractBasicDao.DEFAULT_LIMIT);
    private static final Pattern FORMAT_PARAMETER_PATTERN = Pattern.compile("(?:^|&)format=[\\w-]+?(?:$|&)");

    public static ASTNode parseRQLtoAST(String queryString) throws InvalidRQLException {
        return parseRQLtoAST(queryString, new Converter());
    }

    /**
     * Create an AST Node from the RQL query in the request
     * @param queryString
     * @return ASTNode
     * @throws InvalidRQLRestException
     */
    public static ASTNode parseRQLtoAST(String queryString, Converter converter) throws InvalidRQLException {
        if (queryString == null || queryString.isEmpty()) return DEFAULT_NODE;

        // remove format=x e.g. format=csv parameter
        queryString = FORMAT_PARAMETER_PATTERN.matcher(queryString).replaceFirst("");
        if (queryString.isEmpty()) return DEFAULT_NODE;

        RQLParser parser = new RQLParser();
        try {
            return parser.parse(queryString);
        } catch(IllegalArgumentException | RQLParserException e) {
            throw new InvalidRQLException(e, queryString);
        }
    }

    /**
     * Append an AND Restriction to a query
     * @param query - can be null
     * @param restriction
     * @return
     */
    public static ASTNode addAndRestriction(ASTNode query, ASTNode restriction){
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
    public static ASTNode addOrRestriction(ASTNode query, ASTNode restriction){
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
