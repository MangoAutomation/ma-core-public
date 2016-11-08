/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.appender;

import java.util.ArrayList;
import java.util.List;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.RQLToSQLParseException;
import com.infiniteautomation.mango.db.query.SQLConstants;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;

/**
 * @author Terry Packer
 *
 */
public class GenericSQLColumnQueryAppender implements SQLConstants, SQLColumnQueryAppender{

	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLColumnQueryAppender#appendSQL(com.infiniteautomation.mango.db.query.SQLQueryColumn, java.lang.StringBuilder, java.lang.StringBuilder, java.util.List, java.util.List)
	 */
	@Override
	public void appendSQL(SQLQueryColumn column,
			StringBuilder selectSql, StringBuilder countSql,
			List<Object> selectArgs, List<Object> columnArgs, ComparisonEnum comparison) {

//		if((columnArgs.size() == 1)&&(columnArgs.get(0) == null)){
//			//Catchall for null comparisons
//			appendSQL(column.getName(), IS_SQL, selectSql, countSql);
//			selectArgs.add(null);
//			return;
//		}
		
		switch(comparison){
		case CONTAINS:
			appendSQL(column.getName(), GREATER_THAN_SQL, selectSql, countSql);
			break;
		case EQUAL_TO:
			appendSQL(column.getName(), EQUAL_TO_SQL, selectSql, countSql);
			break;
		case GREATER_THAN:
			appendSQL(column.getName(), GREATER_THAN_SQL, selectSql, countSql);
			break;
		case GREATER_THAN_EQUAL_TO:
			appendSQL(column.getName(), GREATER_THAN_EQUAL_TO_SQL, selectSql, countSql);
			break;
		case IN:
			appendIn(column.getName(), columnArgs, selectSql, countSql);
			break;
		case IS:
			appendSQL(column.getName(), IS_SQL, selectSql, countSql);
			break;
		case IS_NOT:
			appendSQL(column.getName(), IS_NOT_SQL, selectSql, countSql);
			break;
		case LESS_THAN:
			appendSQL(column.getName(), LESS_THAN_SQL, selectSql, countSql);
			break;
		case LESS_THAN_EQUAL_TO:
			appendSQL(column.getName(), LESS_THAN_EQUAL_TO_SQL, selectSql, countSql);
			break;
		case LIKE:
			//Replace wildcards
			appendSQL(column.getName(), LIKE_SQL, selectSql, countSql);
			List<Object> likeArgs = new ArrayList<Object>();
			for(Object o : columnArgs){
				String arg = (String)o;
				arg = arg.replace(STAR, PERCENT);
				likeArgs.add(arg);
			}
			selectArgs.addAll(likeArgs);
			return;
		case NOT_EQUAL_TO:
			appendSQL(column.getName(), NOT_EQUAL_TO_SQL, selectSql, countSql);
			break;
		default:
			throw new RQLToSQLParseException("Unsupported comparison type: " + comparison);
		}

		selectArgs.addAll(columnArgs);
		return;
		
	}
	
	
	
	/**
	 * @param name
	 * @param greaterThanSql
	 * @param selectSql
	 * @param countSql
	 */
	protected void appendSQL(String name, String condition,
			StringBuilder selectSql, StringBuilder countSql) {
		
		selectSql.append(name);
		selectSql.append(condition);
		selectSql.append(SPACE);
		
		countSql.append(name);
		countSql.append(condition);
		countSql.append(SPACE);
		
	}
	
	/**
	 * @param name
	 * @param columnArgs
	 */
	protected void appendIn(String name, List<Object> arguments, StringBuilder selectSql, StringBuilder countSql) {
	
		selectSql.append(name);
		countSql.append(name);
		
		selectSql.append(IN_SQL);
		countSql.append(IN_SQL);

		for(int i=0; i<arguments.size(); i++){
			if(i < arguments.size() - 1){
				selectSql.append(QMARK_COMMA);
				countSql.append(QMARK_COMMA);
			}else{
				selectSql.append(QMARK);
				countSql.append(QMARK);
			}
		}

		selectSql.append(CLOSE_PARENTH);
		countSql.append(CLOSE_PARENTH);
		
		selectSql.append(SPACE);
		countSql.append(SPACE);
		
	}
	
}
