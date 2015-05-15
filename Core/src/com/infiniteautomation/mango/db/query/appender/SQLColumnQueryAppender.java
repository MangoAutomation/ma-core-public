/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.appender;

import java.util.List;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;

/**
 * @author Terry Packer
 *
 */
public interface SQLColumnQueryAppender {

	public static final String NULL = "null";
	
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
	
	public static final String SPACE = " ";
	
	/**
	 * @param sqlQueryColumn
	 * @param selectSql
	 * @param countSql
	 * @param selectArgs
	 * @param columnArgs
	 */
	void appendSQL(SQLQueryColumn column, StringBuilder selectSql,
			StringBuilder countSql, List<Object> selectArgs,
			List<Object> columnArgs, ComparisonEnum comparison);

}
