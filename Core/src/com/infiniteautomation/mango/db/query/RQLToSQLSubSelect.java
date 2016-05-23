/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.HashMap;
import java.util.Map;

import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.ASTVisitor;

/**
 * @author Terry Packer
 *
 */
public class RQLToSQLSubSelect<T> implements SQLConstants, ASTVisitor<SQLSubQuery, SQLSubQuery>{
	
	private AbstractBasicDao<T> dao;
	//Map of any model members to either columns or Vo members
	// they will get translated into columns by the dao in this class
	private Map<String,String> propertyMap;
	private Map<String, SQLColumnQueryAppender> columnAppenders;
	
	public RQLToSQLSubSelect(AbstractBasicDao<T> dao, Map<String,String> propertyMap, Map<String, SQLColumnQueryAppender> appenders){
		this.dao = dao;
		this.propertyMap = propertyMap;
		this.columnAppenders = appenders;
	}
	
	public RQLToSQLSubSelect(AbstractBasicDao<T> dao){
		this.dao = dao;
		this.propertyMap = new HashMap<String,String>();
		this.columnAppenders = new HashMap<String, SQLColumnQueryAppender>();
	}
	
	
	/* (non-Javadoc)
	 * @see net.jazdw.rql.parser.ASTVisitor#visit(net.jazdw.rql.parser.ASTNode, java.lang.Object)
	 */
	@Override
	public SQLSubQuery visit(ASTNode node, SQLSubQuery statement) {
		
        switch (node.getName()) {
        case "and":
        case "or":
            return visitAndOr(node, statement);
        case "eq":
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
        	statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.NOT_EQUAL_TO);
        	return statement;
        case "match":
        case "like":
        	statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.LIKE);
        	return statement;
        case "in":
        	statement.appendColumnQuery(getQueryColumn((String)node.getArgument(0)), node.getArguments().subList(1, node.getArgumentsSize()), ComparisonEnum.IN);
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
	protected SQLSubQuery applySort(ASTNode node, SQLSubQuery statement) {
		
		if(node.getArgumentsSize() == 0)
			return statement;
		
		boolean descending = false;
		
		for (Object arg : node) {
            
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
	

	protected SQLSubQuery visitAndOr(ASTNode node, SQLSubQuery masterStatement) {
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
