/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

/**
 * @author Terry Packer
 *
 */
public class SQLConstants {

	protected static final String SPACE = " ";
	protected static final String WHERE = "WHERE ";
	protected static final String LIMIT_SQL = "LIMIT ?";
	protected static final String OFFSET_SQL = "OFFSET ?";
	protected static final String LIMIT_OFFSET_SQL = "LIMIT ? OFFSET ?";
	protected static final String ORDER_BY = "ORDER BY ";
	protected static final String ASC = " ASC ";
	protected static final String DESC = " DESC ";
	protected static final String COMMA = ",";
	
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
	
}
