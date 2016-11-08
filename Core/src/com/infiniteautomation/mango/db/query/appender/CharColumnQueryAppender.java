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

		//Catchall for null comparisons
		if((columnArgs.size() == 1)&&(columnArgs.get(0) == null)){
			if(comparison == ComparisonEnum.IS)
				super.appendSQL(column.getName(), IS_SQL, selectSql, countSql);
			else if(comparison == ComparisonEnum.IS_NOT)
				super.appendSQL(column.getName(), IS_NOT_SQL, selectSql, countSql);
			else
				super.appendSQL(column.getName(), IS_SQL, selectSql, countSql);
			selectArgs.add(null);
			return;
		}
		
		List<Object> arguments = new ArrayList<Object>();
		for(Object o : columnArgs){
			if(o instanceof Boolean){
				if((Boolean)o)
					arguments.add(Y);
				else
					arguments.add(N);
			}else if (o instanceof String){	
				String arg = (String)o;
				arg = arg.replace(TRUE_L, Y);
				arg = arg.replace(FALSE_L, N);
				arguments.add(arg);
			}
		}
		super.appendSQL(column, selectSql, countSql, selectArgs, arguments, comparison);
	}
	
	
}
