/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.appender;

import java.util.ArrayList;
import java.util.List;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.SQLQueryColumn;
import com.serotonin.m2m2.util.ExportCodes;

/**
 * @author Terry Packer
 *
 */
public class ExportCodeColumnQueryAppender extends GenericSQLColumnQueryAppender{

	private ExportCodes codes;
	
	public ExportCodeColumnQueryAppender(ExportCodes codes){
		this.codes = codes;
	}
	
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
		
		//Map the String name to the Integer ID of the code
		List<Object> arguments = new ArrayList<Object>();
		for(Object o : columnArgs){
			arguments.add(this.codes.getId((String)o));
		}
		
		super.appendSQL(column, selectSql, countSql, selectArgs, arguments, comparison);
	}
	
}
