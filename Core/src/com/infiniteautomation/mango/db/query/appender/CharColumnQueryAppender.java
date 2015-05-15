/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.appender;

import java.util.ArrayList;
import java.util.List;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;

/**
 * @author Terry Packer
 *
 */
public class CharColumnQueryAppender extends GenericSQLColumnQueryAppender{

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
		
		List<Object> arguments = new ArrayList<Object>();
		for(Object o : columnArgs){
			String arg = (String)o;
			arg = arg.toLowerCase();
			arg = arg.replace(TRUE_L, Y);
			arg = arg.replace(FALSE_L, N);
			arguments.add(arg);
		}
		super.appendSQL(column, selectSql, countSql, selectArgs, arguments, comparison);
	}
	
	
}
