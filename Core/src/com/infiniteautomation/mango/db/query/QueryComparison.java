/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.m2m2.util.ExportCodes;


/**
 * @author Terry Packer
 *
 */
public class QueryComparison {

	public static final int EQUAL_TO = 1;
	public static final int NOT_EQUAL_TO = 2;
	public static final int LESS_THAN = 3;
	public static final int LESS_THAN_EQUAL_TO = 4;
	public static final int GREATER_THAN = 5;
	public static final int GREATER_THAN_EQUAL_TO = 6;
	public static final int IN = 7;
	public static final int MATCH = 8;
	public static final int CONTAINS = 9;
	
	public static final ExportCodes COMPARISON_TYPE_CODES = new ExportCodes();
	static{
		COMPARISON_TYPE_CODES.addElement(EQUAL_TO, "EQUAL_TO");
		COMPARISON_TYPE_CODES.addElement(NOT_EQUAL_TO, "NOT_EQUAL_TO");
		COMPARISON_TYPE_CODES.addElement(LESS_THAN, "LESS_THAN");
		COMPARISON_TYPE_CODES.addElement(LESS_THAN_EQUAL_TO, "LESS_THAN_EQUAL_TO");
		COMPARISON_TYPE_CODES.addElement(GREATER_THAN, "GREATER_THAN");
		COMPARISON_TYPE_CODES.addElement(GREATER_THAN_EQUAL_TO, "GREATER_THAN_EQUAL_TO");
		COMPARISON_TYPE_CODES.addElement(IN, "IN");
		COMPARISON_TYPE_CODES.addElement(MATCH, "MATCH");
		COMPARISON_TYPE_CODES.addElement(CONTAINS, "CONTAINS");
	}
	
	
	private String attribute;
	private int comparisonType;
	private String condition;
	
	public QueryComparison() { }
	
	public QueryComparison(String attribute, int comparisonType, String condition){
		this.attribute = attribute;
		this.comparisonType = comparisonType;
		this.condition = condition;
	}

	public String getAttribute() {
		return attribute;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	@JsonIgnore
	public int getComparisonType() {
		return comparisonType;
	}

	@JsonIgnore
	public void setComparisonType(int comparisonType) {
		this.comparisonType = comparisonType;
	}
	
	public String getComparison(){
		return COMPARISON_TYPE_CODES.getCode(this.comparisonType);
	}
	public void setComparison(String comparisonCode){
		this.comparisonType = COMPARISON_TYPE_CODES.getId(comparisonCode);
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	

	public String generateSql(int sqlType, String column, String tablePrefix){
		StringBuilder sql = new StringBuilder();
		
		if(!StringUtils.isEmpty(tablePrefix)){
			column = tablePrefix + column;
		}
		
		switch(sqlType){
		case Types.VARCHAR:
		case Types.LONGVARCHAR:
			return generateStringSql(sql, column);
		case Types.BOOLEAN:
			return generateBooleanSql(sql, column);
		case Types.CHAR: //Binary
			return generateCharSql(sql, column);
		case Types.INTEGER:
			return generateIntegerSql(sql, column);
		case Types.BIGINT:
			return generateBigIntegerSql(sql, column);
		}
        return sql.toString();
	}	
	
	/**
	 * @param sql
	 * @param column
	 * @return
	 */
	private String generateCharSql(StringBuilder sql, String column) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param sql
	 * @param column
	 * @return
	 */
	private String generateBooleanSql(StringBuilder sql, String column) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param sql
	 * @param column
	 * @return
	 */
	private String generateStringSql(StringBuilder sql, String column) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param sql
	 * @param column
	 * @return
	 */
	private String generateBigIntegerSql(StringBuilder sql, String column) {
		//We could either be a Date or a Long
		String bigIntString;
		try{
			Long.parseLong(condition);
			bigIntString = condition;
		}catch(Exception e){
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			try {
				Date d = sdf.parse(condition);
				bigIntString = Long.toString(d.getTime());
			} catch (ParseException e1) {
				//TODO deal with this
				e1.printStackTrace();
				return " FAIL! ";
			}
		} 
		
		switch(comparisonType){
			case GREATER_THAN:
				sql.append(column);
				sql.append(">");
				sql.append(bigIntString);
			break;
			case GREATER_THAN_EQUAL_TO:
				sql.append(column);
				sql.append(">=");
				sql.append(bigIntString);
			break;
			case LESS_THAN:
				sql.append(column);
				sql.append("<");
				sql.append(bigIntString);
			break;
			case LESS_THAN_EQUAL_TO:
				sql.append(column);
				sql.append("<=");
				sql.append(bigIntString);
			break;
	    	case EQUAL_TO:
				sql.append(column);
				sql.append("=");
				sql.append(bigIntString);
			break;
	    	case NOT_EQUAL_TO:
				sql.append(column);
				sql.append("!=");
				sql.append(bigIntString);
			break;
			default:
		}
		return sql.toString();
	}

	protected String generateIntegerSql(StringBuilder sql, String column){
		switch(comparisonType){
		case GREATER_THAN:
			sql.append(column);
			sql.append(">");
			sql.append(condition);
		break;
		case GREATER_THAN_EQUAL_TO:
			sql.append(column);
			sql.append(">=");
			sql.append(condition);
		break;
		case LESS_THAN:
			sql.append(column);
			sql.append("<");
			sql.append(condition);
		break;
		case LESS_THAN_EQUAL_TO:
			sql.append(column);
			sql.append("<=");
			sql.append(condition);
		break;
    	case EQUAL_TO:
			sql.append(column);
			sql.append("=");
			sql.append(condition);
		break;
    	case NOT_EQUAL_TO:
			sql.append(column);
			sql.append("!=");
			sql.append(condition);
		break;
		default:
	}
        
        return sql.toString();
	}
	
}
