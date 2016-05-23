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
public interface SQLColumnQueryAppender{

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
