/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.lang.reflect.InvocationTargetException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
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
	public static final int LIKE = 8;
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
		COMPARISON_TYPE_CODES.addElement(LIKE, "LIKE");
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
	@JsonGetter("comparisonType")
	public String getComparison(){
		return COMPARISON_TYPE_CODES.getCode(this.comparisonType);
	}
	@JsonSetter("comparisonType")
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
		String bool = "N";
		if(condition.equalsIgnoreCase("true")){
			bool = "Y";
		}
		switch(comparisonType){
			case GREATER_THAN:
				sql.append(column);
				sql.append(">");
				sql.append("'");
				sql.append(bool);
				sql.append("'");
			break;
			case GREATER_THAN_EQUAL_TO:
				sql.append(column);
				sql.append(">=");
				sql.append("'");
				sql.append(bool);
				sql.append("'");
			break;
			case LESS_THAN:
				sql.append(column);
				sql.append("<");
				sql.append("'");
				sql.append(bool);
				sql.append("'");
			break;
			case LESS_THAN_EQUAL_TO:
				sql.append(column);
				sql.append("<=");
				sql.append("'");
				sql.append(bool);
				sql.append("'");
			break;
			case LIKE:
	    	case EQUAL_TO:
				sql.append(column);
				sql.append("=");
				sql.append("'");
				sql.append(bool);
				sql.append("'");
			break;
	    	case NOT_EQUAL_TO:
				sql.append(column);
				sql.append("!=");
				sql.append("'");
				sql.append(bool);
				sql.append("'");
			break;
			default:
		}
		return sql.toString();
	}

	/**
	 * @param sql
	 * @param column
	 * @return
	 */
	private String generateBooleanSql(StringBuilder sql, String column) {
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
		case LIKE:
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

	/**
	 * @param sql
	 * @param column
	 * @return
	 */
	private String generateStringSql(StringBuilder sql, String column) {
		switch(comparisonType){
			case GREATER_THAN:
				sql.append(column);
				sql.append(">");
				sql.append("'");
				sql.append(condition);
				sql.append("'");
			break;
			case GREATER_THAN_EQUAL_TO:
				sql.append(column);
				sql.append(">=");
				sql.append("'");
				sql.append(condition);
				sql.append("'");
			break;
			case LESS_THAN:
				sql.append(column);
				sql.append("<");
				sql.append("'");
				sql.append(condition);
				sql.append("'");
			break;
			case LESS_THAN_EQUAL_TO:
				sql.append(column);
				sql.append("<=");
				sql.append("'");
				sql.append(condition);
				sql.append("'");
			break;
	    	case EQUAL_TO:
				sql.append(column);
				sql.append("=");
				sql.append("'");
				sql.append(condition);
				sql.append("'");
			break;
	    	case NOT_EQUAL_TO:
				sql.append(column);
				sql.append("!=");
				sql.append("'");
				sql.append(condition);
				sql.append("'");
			break;
	    	case LIKE:
                switch (Common.databaseProxy.getType()) {
                case MYSQL:
                case POSTGRES:
                case MSSQL:
                case H2:
                	sql.append("lower(");
                	sql.append(column);
                	sql.append(") LIKE '");
                	sql.append(condition.toLowerCase());
                	sql.append("'");
                break;
                case DERBY:
                	sql.append("(CHAR(");
                	sql.append(column);
                	sql.append(") LIKE '");
                	sql.append(condition.toLowerCase());
                	sql.append("'");
                break;
                default:
                	throw new ShouldNeverHappenException("No case for converting regex expressing for database of type: " + Common.databaseProxy.getType());
                }

	    	break;
			default:
		}
		return sql.toString();
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
			case LIKE:
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
			case LIKE:
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
	
	/**
	 * Apply this condition to an object that contains the 
	 * attribute to apply against.  
	 * 
	 * This method uses reflection to extract the value and then
	 * apply the condition.
	 * 
	 * @param instance
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 */
	public boolean apply(Object instance) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InvocationTargetException, NoSuchMethodException{
		Object value = PropertyUtils.getProperty(instance, attribute);
		return this.compare(value);
	}
	
	/**
	 * Compare the value by first determining its type
	 * @param value
	 * @return
	 */
	public boolean compare(Object value){
		if(value instanceof Integer){
			return compareInteger((Integer)value);
		}else if(value instanceof Double){
			return compareDouble((Double)value);
		}else if(value instanceof Long){
			return compareLong((Long)value);
		}else if(value instanceof String){
			return compareString((String)value);
		}else if(value instanceof Boolean){
			return compareBoolean((Boolean)value);
		}else{
			throw new ShouldNeverHappenException("Unsupported class type: " + value.getClass().getCanonicalName());
		}
	}
	
	public boolean compareInteger(Integer value){
		try{
			Integer thisValue = Integer.parseInt(condition);
			switch(comparisonType){
				case GREATER_THAN:
					return thisValue > value;
				case GREATER_THAN_EQUAL_TO:
					return thisValue >= value;
				case LESS_THAN:
					return thisValue < value;
				case LESS_THAN_EQUAL_TO:
					return thisValue <= value;
				case LIKE:
		    	case EQUAL_TO:
					return thisValue == value;
		    	case NOT_EQUAL_TO:
					return thisValue != value;
				default:
					throw new ShouldNeverHappenException("Unsupported comparisonType: " + getComparison());
			}
		}catch(NumberFormatException e){
			//Munchy munch
			return false;
		}
	}

	public boolean compareDouble(Double value){
		try{
			Double thisValue = Double.parseDouble(condition);
			switch(comparisonType){
				case GREATER_THAN:
					return thisValue > value;
				case GREATER_THAN_EQUAL_TO:
					return thisValue >= value;
				case LESS_THAN:
					return thisValue < value;
				case LESS_THAN_EQUAL_TO:
					return thisValue <= value;
				case LIKE:
		    	case EQUAL_TO:
					return thisValue == value;
		    	case NOT_EQUAL_TO:
					return thisValue != value;
				default:
					throw new ShouldNeverHappenException("Unsupported comparisonType: " + getComparison());
			}
		}catch(NumberFormatException e){
			//Munchy munch
			return false;
		}
	}
	
	public boolean compareLong(Long value){
		try{
			Long thisValue = Long.parseLong(condition);
			switch(comparisonType){
				case GREATER_THAN:
					return thisValue > value;
				case GREATER_THAN_EQUAL_TO:
					return thisValue >= value;
				case LESS_THAN:
					return thisValue < value;
				case LESS_THAN_EQUAL_TO:
					return thisValue <= value;
				case LIKE:
		    	case EQUAL_TO:
					return thisValue == value;
		    	case NOT_EQUAL_TO:
					return thisValue != value;
				default:
					throw new ShouldNeverHappenException("Unsupported comparisonType: " + getComparison());
			}
		}catch(NumberFormatException e){
			//Munchy munch
			return false;
		}
	}
	
	public boolean compareString(String value){
		switch(comparisonType){
			case GREATER_THAN:
				return condition.compareTo(value) > 0;
			case GREATER_THAN_EQUAL_TO:
				return condition.compareTo(value) >= 0;
			case LESS_THAN:
				return condition.compareTo(value) < 0;
			case LESS_THAN_EQUAL_TO:
				return condition.compareTo(value) <= 0;
			case LIKE:
				//Create regex by simply replacing % by .*
				String regex = condition.replace("%", ".*");
				return condition.matches(regex);
	    	case EQUAL_TO:
	    		return condition.equals(value);
	    	case NOT_EQUAL_TO:
	    		return !condition.equals(value);
			default:
				throw new ShouldNeverHappenException("Unsupported comparisonType: " + getComparison());
		}
	}
	public boolean compareBoolean(Boolean value){
		try{
			Boolean thisValue = Boolean.parseBoolean(condition);
			switch(comparisonType){
				case GREATER_THAN:
					return thisValue.compareTo(value) > 0;
				case GREATER_THAN_EQUAL_TO:
					return thisValue.compareTo(value) >= 0;
				case LESS_THAN:
					return thisValue.compareTo(value) < 0;
				case LESS_THAN_EQUAL_TO:
					return thisValue.compareTo(value) <= 0;
				case LIKE:
		    	case EQUAL_TO:
					return thisValue == value;
		    	case NOT_EQUAL_TO:
					return thisValue != value;
				default:
					throw new ShouldNeverHappenException("Unsupported comparisonType: " + getComparison());
			}
		}catch(NumberFormatException e){
			//Munchy munch
			return false;
		}
	}
	
	public String toString(){
		StringBuilder builder = new StringBuilder();
		
		builder.append(this.attribute);
		builder.append(" ");

		builder.append(COMPARISON_TYPE_CODES.getCode(this.comparisonType));
		builder.append(" ");

		builder.append(this.condition);
		return builder.toString();
	}
}
