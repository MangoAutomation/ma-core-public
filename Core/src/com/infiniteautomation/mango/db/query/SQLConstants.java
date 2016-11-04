/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

/**
 * @author Terry Packer
 *
 */
public interface SQLConstants {

	public static final String SPACE = " ";
	public static final String WHERE = "WHERE ";
	public static final String LIMIT_SQL = "LIMIT ?";
	public static final String OFFSET_SQL = "OFFSET ?";
	public static final String LIMIT_OFFSET_SQL = "LIMIT ? OFFSET ?";
	public static final String ORDER_BY = "ORDER BY ";
	public static final String ASC = " ASC ";
	public static final String DESC = " DESC ";
	public static final String COMMA = ",";
	public static final String JOIN = "JOIN ";
	public static final String LEFT_JOIN = "LEFT JOIN ";
	public static final String ON = " ON ";
	
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
	public static final String IS_NOT_SQL = " IS NOT ?";
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
	
	public static final String NULL = "null";
	
	/* DB Specific */
	public static final String DERBY_LIKE = " LIKE ? ";
	public static final String H2_LIKE = " LIKE ? ";
	public static final String DERBY_CHAR = "(CHAR(";
	public static final String DERBY_NOT_LIKE = ") NOT LIKE ? ) ";
	public static final String H2_LOWER = "LOWER(";
	public static final String H2_NOT_LIKE = ") NOT LIKE ? ";
	
	public static final int EQUAL_TO = 1;
	public static final int NOT_EQUAL_TO = 2;
	public static final int LESS_THAN = 3;
	public static final int LESS_THAN_EQUAL_TO = 4;
	public static final int GREATER_THAN = 5;
	public static final int GREATER_THAN_EQUAL_TO = 6;
	public static final int IN = 7;
	public static final int LIKE = 8;
	public static final int CONTAINS = 9;
	
}
