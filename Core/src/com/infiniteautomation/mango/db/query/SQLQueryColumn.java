/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.sql.Types;
import java.util.List;

import com.infiniteautomation.mango.db.query.appender.BigIntegerColumnQueryAppender;
import com.infiniteautomation.mango.db.query.appender.CharColumnQueryAppender;
import com.infiniteautomation.mango.db.query.appender.GenericSQLColumnQueryAppender;
import com.infiniteautomation.mango.db.query.appender.SQLColumnQueryAppender;
import com.infiniteautomation.mango.db.query.appender.StringColumnQueryAppender;

/**
 * @author Terry Packer
 *
 */
public class SQLQueryColumn {

	private String name;
	private int sqlType;
	private SQLColumnQueryAppender appender;

	/**
	 * @param dbCol
	 * @param sqlType2
	 */
	public SQLQueryColumn(String dbCol, int sqlType) {
		this.name = dbCol;
		this.sqlType = sqlType;
		
		switch(sqlType){
		case Types.VARCHAR:
		case Types.LONGVARCHAR:
			this.appender = new StringColumnQueryAppender();
			break;
		case Types.CHAR: //Binary
			this.appender = new CharColumnQueryAppender();
		break;
		case Types.BIGINT:
			this.appender = new BigIntegerColumnQueryAppender();
		break;
		default:
		case Types.BOOLEAN:
		case Types.INTEGER:
			this.appender = new GenericSQLColumnQueryAppender();
		}
	}
	
	
	/**
	 * @param dbCol
	 * @param sqlType2
	 */
	public SQLQueryColumn(String dbCol, int sqlType, SQLColumnQueryAppender appender) {
		this.name = dbCol;
		this.sqlType = sqlType;
		this.appender = appender;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSqlType() {
		return sqlType;
	}

	public void setSqlType(int sqlType) {
		this.sqlType = sqlType;
	}


	/**
	 * @param selectSql
	 * @param countSql
	 * @param selectArgs
	 * @param columnArgs
	 */
	public void appendSQL(StringBuilder selectSql, StringBuilder countSql,
			List<Object> selectArgs, List<Object> columnArgs, ComparisonEnum comparison) {
		this.appender.appendSQL(this, selectSql, countSql, selectArgs, columnArgs, comparison);
	}


	/**
	 * @param appender2
	 */
	public void setAppender(SQLColumnQueryAppender appender) {
		this.appender = appender;
	}
	

}
