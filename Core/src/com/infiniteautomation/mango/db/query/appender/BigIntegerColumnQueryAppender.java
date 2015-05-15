/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.appender;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;
import com.serotonin.ShouldNeverHappenException;

/**
 * @author Terry Packer
 *
 */
public class BigIntegerColumnQueryAppender extends GenericSQLColumnQueryAppender{

	private static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLColumnQueryAppender#appendSQL(com.infiniteautomation.mango.db.query.SQLQueryColumn, java.lang.StringBuilder, java.lang.StringBuilder, java.util.List, java.util.List)
	 */
	@Override
	public void appendSQL(SQLQueryColumn column,
			StringBuilder selectSql, StringBuilder countSql,
			List<Object> selectArgs, List<Object> columnArgs, ComparisonEnum comparison) {

		
		if((columnArgs.size() == 1)&&(((String)columnArgs.get(0)).equalsIgnoreCase(NULL))){
			//Catchall for null comparisons
			appendSQL(column.getName(), IS_SQL, selectSql, countSql);
			selectArgs.add(NULL);
			return;
		}
		
		if(columnArgs.size() == 1){
			try{
				//Are we a long value
				Long.parseLong((String)columnArgs.get(0));
				
			}catch(Exception e){
				SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
				try {
					Date d = sdf.parse((String)columnArgs.get(0));
					columnArgs.set(0, Long.toString(d.getTime()));
				} catch (ParseException e1) {
					throw new ShouldNeverHappenException(e);
				}
			} 
		}
		super.appendSQL(column, selectSql, countSql, selectArgs, columnArgs, comparison);
	}
}
