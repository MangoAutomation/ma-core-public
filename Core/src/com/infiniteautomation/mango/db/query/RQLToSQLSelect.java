/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.ASTVisitor;

import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;

/**
 * Class to parse RQL into SQL Statements
 * 
 * All methods can throw RQLToSQLParseExceptions
 * 
 * 
 * @author Terry Packer
 *
 */
public class RQLToSQLSelect<T extends AbstractBasicVO> implements SQLConstants, ASTVisitor<SQLStatement, SQLStatement>{
	
	private AbstractBasicDao<T> dao;
	//Map of any model members to either columns or Vo members
	// they will get translated into columns by the dao in this class
	private Map<String,String> propertyMap;
	private Map<String, SQLColumnQueryAppender> columnAppenders;
	
	public RQLToSQLSelect(AbstractBasicDao<T> dao, Map<String,String> propertyMap, Map<String, SQLColumnQueryAppender> appenders){
		this.dao = dao;
		this.propertyMap = propertyMap;
		this.columnAppenders = appenders;
	}
	
	public RQLToSQLSelect(AbstractBasicDao<T> dao){
		this.dao = dao;
		this.propertyMap = new HashMap<String,String>();
		this.columnAppenders = new HashMap<String, SQLColumnQueryAppender>();
	}
	
	
	/* (non-Javadoc)
	 * @see net.jazdw.rql.parser.ASTVisitor#visit(net.jazdw.rql.parser.ASTNode, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public SQLStatement visit(ASTNode node, SQLStatement statement) {
		
        switch (node.getName()) {
        case "and":
        case "or":
            return visitAndOr(node, statement);
        case "eq":
        	//Null Check
    		if(node.getArgument(1) == null)
    			statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.IS);
    		else
    			statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.EQUAL_TO);
        	return statement;
        case "gt":
        	statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.GREATER_THAN);
        	return statement;
        case "ge":
        	statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.GREATER_THAN_EQUAL_TO);
        	return statement;
        case "lt":
        	statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.LESS_THAN);
        	return statement;
        case "le":
        	statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.LESS_THAN_EQUAL_TO);
        	return statement;
        case "ne":
        	//Null Check
    		if(node.getArgument(1) == null)
    			statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.IS_NOT);
    		else
    			statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.NOT_EQUAL_TO);
        	return statement;
        case "match":
        case "like":
    		//Null Check
    		if(node.getArgument(1) == null){
    			statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.IS);
    		}else{
	        	String pattern = (String)node.getArgument(1);
	        	if(pattern.startsWith("^")){
	        		List<Object> args = new ArrayList<Object>();
	        		String arg = pattern.substring(1);
	        		if(arg.equalsIgnoreCase("null")){
		        		args.add(null);
	        			statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), args, ComparisonEnum.IS_NOT);
	        		}else{
		        		args.add(arg);
	        			statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), args, ComparisonEnum.NOT_LIKE);
	        		}
	        	}else
	        		statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.LIKE);
    		}
        	return statement;
        case "in":
            Object firstArg = node.getArgument(1);
            List<Object> inArray;
            if (firstArg instanceof List) {
                inArray = (List<Object>) firstArg;
            } else {
               inArray = node.getArguments().subList(1, node.getArgumentsSize());
            }
            //Since MySQL does not support empty IN () values
            if(inArray.size() == 0) {
                //Query on 1 = 2, which can never be true, same as the column value never being in an empty set 
                SQLQueryColumn one = new SQLQueryColumn("1", Types.INTEGER);
                statement.appendColumnQuery(one, Arrays.asList(2), ComparisonEnum.EQUAL_TO);
            }else
                statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), inArray, ComparisonEnum.IN);
        	return statement;
        case "sort":
            return applySort(node, statement);
        case "limit":
            statement.applyLimit(node.getArguments());
            return statement;
        default:
        }

        throw new RQLToSQLParseException("Unsupported operation: " + node.getName());
	}

	/**
	 * Translate the property argument into the column name/sql statement required
	 * 
	 * @param argument
	 * @return
	 */
	protected SQLQueryColumn getQueryColumn(String argument) {

		//Check our local map and translate first
		String prop = this.propertyMap.get(argument);
		SQLColumnQueryAppender appender;
		SQLQueryColumn column;
		if(prop == null){
			appender = this.columnAppenders.get(argument);
			column = dao.getQueryColumn(argument);
		}else{
			appender = this.columnAppenders.get(prop);
			if(appender == null)
				appender = this.columnAppenders.get(argument);
			column = dao.getQueryColumn(prop);
		}
		if(appender != null)
			column.setAppender(appender);
		return column;
		
	}

	/**
	 * @param queryColumn
	 * @param subList
	 * @param statement
	 * @return
	 */
	protected SQLStatement applySort(ASTNode node, SQLStatement statement) {
		
		if(node.getArgumentsSize() == 0)
			return statement;
		
		
		
		for (Object arg : node) {
			boolean descending = false;
            String prop = (String) arg;
            if (prop.startsWith("-")) {
            	descending = true;
                prop = prop.substring(1);
            } else if (prop.startsWith("+")) {
                prop = prop.substring(1);
                descending = false;
            }
            
            SQLQueryColumn column = this.getQueryColumn(prop);
            statement.applySort(column, descending);
		}
		return statement;
	}

	/**
	 * @param node
	 * @param param
	 * @return
	 */
	protected SQLStatement visitAndOr(ASTNode node, SQLStatement masterStatement) {
		boolean opened = false;
		ComparisonEnum comparison = ComparisonEnum.convertTo(node.getName());
		
		for (Object obj : node) {
            if (obj instanceof ASTNode) {
            	
            	switch(((ASTNode) obj).getName()){
            	case "limit":
            	case "sort":
                	((ASTNode) obj).accept(this, masterStatement);
            		break;
            	default:
            		if(!opened){
            			masterStatement.openAndOr(comparison);
            			opened = true;
            		}

                	//We know we are in an And/Or 
                	((ASTNode) obj).accept(this, masterStatement);

            	}
            } else {
                throw new RQLToSQLParseException("AND/OR terms should only have ASTNode arguments");
            }
        }
		
		if(opened)
			masterStatement.closeAndOr(comparison);
		
		return masterStatement;
	}
}
