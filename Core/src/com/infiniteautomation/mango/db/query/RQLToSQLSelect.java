/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.ASTVisitor;

import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;

/**
 * Class to parse RQL into SQL Statements
 * 
 * All methods can throw RQLToSQLParseExceptions
 * 
 * 
 * @author Terry Packer
 *
 */
public class RQLToSQLSelect<T> implements ASTVisitor<SQLStatement, SQLStatement>{
	
	public static final int EQUAL_TO = 1;
	public static final int NOT_EQUAL_TO = 2;
	public static final int LESS_THAN = 3;
	public static final int LESS_THAN_EQUAL_TO = 4;
	public static final int GREATER_THAN = 5;
	public static final int GREATER_THAN_EQUAL_TO = 6;
	public static final int IN = 7;
	public static final int LIKE = 8;
	public static final int CONTAINS = 9;
	
	public static final String EQUAL_TO_SQL = "=?";
	public static final String NOT_EQUAL_TO_SQL = "!=?";
	public static final String LESS_THAN_SQL = "<?";
	public static final String LESS_THAN_EQUAL_TO_SQL = "<=?";
	public static final String GREATER_THAN_SQL = ">?";
	public static final String GREATER_THAN_EQUAL_TO_SQL = ">=?";
	public static final String IN_SQL = " IN (";
	public static final String LIKE_SQL = " LIKE ?";
	public static final String CONTAINS_SQL = " CONTAINS ?";
	
	public static final String QMARK = "?";
	public static final String IS_SQL = "IS ?";
	public static final String QMARK_COMMA = "?,";
	public static final String OPEN_PARENTH = "(";
	public static final String CLOSE_PARENTH = ")";
	public static final String STAR = "*";
	public static final String PERCENT = "%";
	public static final String TRUE = "TRUE";
	public static final String TRUE_L = "true";
	public static final String FALSE = "FALSE";
	public static final String FALSE_L = "false";
	public static final String Y = "Y";
	public static final String N = "N";
	
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
	@Override
	public SQLStatement visit(ASTNode node, SQLStatement statement) {
		
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
	private SQLQueryColumn getQueryColumn(String argument) {

		//Check our local map and translate first
		String prop = this.propertyMap.get(argument);
		SQLColumnQueryAppender appender;
		SQLQueryColumn column;
		if(prop == null){
			appender = this.columnAppenders.get(argument);
			column = dao.getQueryColumn(argument);
		}else{
			appender = this.columnAppenders.get(prop);
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
	private SQLStatement applySort(ASTNode node, SQLStatement statement) {
		
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
            
            SQLQueryColumn column = dao.getQueryColumn(prop);
            statement.applySort(column, descending);
		}
		return statement;
	}

	/**
	 * @param node
	 * @param param
	 * @return
	 */
	private SQLStatement visitAndOr(ASTNode node, SQLStatement masterStatement) {
		
		boolean opened = false;
		
		int cnt = 0;
		//Count the number of nodes that are viable for AND comparisons
		//TODO Better way to handle this?
		int total = 0;
		for(Object obj : node){
			if (obj instanceof ASTNode) {
            	
            	switch(((ASTNode) obj).getName()){
            	case "limit":
            	case "sort":
            	break;
            	default:
            		total++;
            	}
			}
		}
		for (Object obj : node) {
            if (obj instanceof ASTNode) {
            	
            	switch(((ASTNode) obj).getName()){
            	case "limit":
            	case "sort":
                	((ASTNode) obj).accept(this, masterStatement);
            		break;
            	default:
                	if(!opened){
                		masterStatement.appendSQL(OPEN_PARENTH, new ArrayList<Object>());
                		opened = true;
                	}
                	((ASTNode) obj).accept(this, masterStatement);
                	cnt++;
                	if(cnt < total)
                		masterStatement.appendSQL(node.getName(), new ArrayList<Object>());

            	}
            } else {
                throw new RQLToSQLParseException("AND/OR terms should only have ASTNode arguments");
            }
        }
		
		if(opened)
			masterStatement.appendSQL(CLOSE_PARENTH, new ArrayList<Object>());
		
		return masterStatement;
	}
}
