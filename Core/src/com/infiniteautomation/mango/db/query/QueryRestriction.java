/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terry Packer
 *
 */
public class QueryRestriction {
	SQLQueryColumn column;
	List<Object> columnArgs;
	ComparisonEnum comparison;
	
	public QueryRestriction(SQLQueryColumn column, List<Object> columnArgs, ComparisonEnum comparison) {
		this.column = column;
		this.columnArgs = columnArgs;
		this.comparison = comparison;
	}
	
	public ComparisonEnum getComparison() {
		return comparison;
	}
	public void setComparison(ComparisonEnum comparison) {
		this.comparison = comparison;
	}
	public SQLQueryColumn getColumn() {
		return column;
	}
	public void setColumn(SQLQueryColumn column) {
		this.column = column;
	}
	public List<Object> getColumnArgs() {
		return columnArgs;
	}
	public void setColumnArgs(List<Object> columnArgs) {
		this.columnArgs = columnArgs;
	}
	
	public String toString(){
		return this.column + this.comparison.name() + this.columnArgs;
	}

	/**
	 * @param selectSql
	 * @param countSql
	 * @param selectArgs
	 * @param countArgs
	 * @param comparison2
	 */
	public void appendSQL(StringBuilder selectSql, StringBuilder countSql, List<Object> selectArgs,
			List<Object> countArgs) {
		List<Object> newArgs = new ArrayList<Object>();
		this.column.appendSQL(selectSql, countSql, newArgs, this.columnArgs, this.comparison);
		selectArgs.addAll(newArgs);
		countArgs.addAll(newArgs);
	}
}
