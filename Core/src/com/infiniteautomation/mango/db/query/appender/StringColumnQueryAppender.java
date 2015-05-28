/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.appender;

import java.util.ArrayList;
import java.util.List;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.RQLToSQLParseException;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;
import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 *
 */
public class StringColumnQueryAppender extends GenericSQLColumnQueryAppender{

	
	private final static String DERBY_CHAR = "(CHAR(";
	private final static String DERBY_LIKE = ") LIKE ? ) ";
	
	private final static String H2_LOWER = "LOWER(";
	private final static String H2_LIKE = ") LIKE ? ";
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLColumnQueryAppender#appendSQL(com.infiniteautomation.mango.db.query.SQLQueryColumn, java.lang.StringBuilder, java.lang.StringBuilder, java.util.List, java.util.List)
	 */
	@Override
	public void appendSQL(SQLQueryColumn column,
			StringBuilder selectSql, StringBuilder countSql,
			List<Object> selectArgs, List<Object> columnArgs, ComparisonEnum comparison) {

		
		if((columnArgs.size() == 1)&&(columnArgs.get(0) == null)){
			//Catchall for null comparisons
			appendSQL(column.getName(), IS_SQL, selectSql, countSql);
			selectArgs.add(NULL);
			return;
		}
		
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
		case LESS_THAN:
			appendSQL(column.getName(), LESS_THAN_SQL, selectSql, countSql);
			break;
		case LESS_THAN_EQUAL_TO:
			appendSQL(column.getName(), LESS_THAN_EQUAL_TO_SQL, selectSql, countSql);
			break;
		case LIKE:
			 switch (Common.databaseProxy.getType()) {
             case MYSQL:
             case POSTGRES:
             case MSSQL:
             case H2:
             	selectSql.append(H2_LOWER);
            	selectSql.append(column.getName());
            	selectSql.append(H2_LIKE);

            	countSql.append(H2_LOWER);
            	countSql.append(column.getName());
            	countSql.append(H2_LIKE);
             break;
             case DERBY:
             	selectSql.append(DERBY_CHAR);
            	selectSql.append(column.getName());
            	selectSql.append(DERBY_LIKE);

            	countSql.append(DERBY_CHAR);
            	countSql.append(column.getName());
            	countSql.append(DERBY_LIKE);
             break;
             default:
             	throw new RQLToSQLParseException("No case for converting LIKE expressing for database of type: " + Common.databaseProxy.getType());
            }
			List<Object> likeArgs = new ArrayList<Object>();
			for(Object o : columnArgs){
				String arg = (String)o;
				arg = arg.replace(STAR, PERCENT);
				//TODO Do we want to be case specific?
				//We use LOWER in the queries so drop the case here
				arg = arg.toLowerCase();
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

}
