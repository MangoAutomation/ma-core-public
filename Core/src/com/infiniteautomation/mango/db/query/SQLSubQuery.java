/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Create a statement of the form
 * 
 * SELECT * FROM (SELECT * FROM rql) JOINS
 * 
 * Where the outer select is the baseSelect member
 * 
 * @author Terry Packer
 *
 */
public class SQLSubQuery extends SQLStatement{

	private String tableName;
	private String tablePrefix;
	
	private SQLStatement baseSelect;
	
	/**
	 * @param baseSelectStatement - SELECT x,y,z
	 * @param selectArgs
	 * @param baseCountStatement
	 * @param applyLimitToSelectSql
	 */
	public SQLSubQuery(String baseSelect, String baseCountStatement, String joins,
			String tableName,
			String tablePrefix,
			boolean applyLimitToSelectSql) {
		super("SELECT * FROM " + tableName + " AS " + tablePrefix, new ArrayList<Object>(), baseCountStatement, null, applyLimitToSelectSql);
		this.baseSelect = new SQLStatement(baseSelect, baseCountStatement, joins, applyLimitToSelectSql);
		this.tableName = tableName;
		this.tablePrefix = tablePrefix;
	}

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#getSelectSql()
	 */
	@Override
	public String getSelectSql() {
		StringBuilder baseSelect = new StringBuilder(this.baseSelect.selectSql);
		
		//Do we have any sub query parameters
		if(super.getSelectArgs().size() > 0){
			baseSelect.append(" ( ");
			if(this.selectArgs.size() > 0)
				baseSelect.append(super.getSelectSql());
			else{
				//Only Apply Limit and Offset
				baseSelect.append(this.selectSql);
				if(this.applyLimit)
					baseSelect.append(limitOffset.toString());
			}
			baseSelect.append(") ");		
		}else{
			baseSelect.append(this.tableName);
		}
		
		if(this.tablePrefix != null){
			baseSelect.append(" AS ");
			baseSelect.append(this.tablePrefix);
			baseSelect.append(SPACE);
		}
		
		if(this.baseSelect.joins != null){
			baseSelect.append(this.baseSelect.joins);
			baseSelect.append(SPACE);
		}
		
		if(this.baseSelect.appliedWhere)
			baseSelect.append(this.baseSelect.selectWhere);
		
		//Apply the Sort
		this.baseSelect.applySort(baseSelect);
		
		if(this.baseSelect.applyLimit)
			baseSelect.append(this.baseSelect.limitOffset.toString());
		
		return baseSelect.toString();
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#getCountSql()
	 */
	@Override
	public String getCountSql() {
		StringBuilder baseCount = new StringBuilder(this.baseSelect.countSql);
		
		if(this.selectArgs.size() > 0){
			baseCount.append(" ( ");
			//Apply where without limit or sort
			baseCount.append(this.selectSql);
			if(this.joins != null)
				baseCount.append(joins);
			baseCount.append(this.selectWhere);
			baseCount.append(") ");
			
		}else{
			baseCount.append(this.tableName);
		}
		
		if(this.tablePrefix != null){
			baseCount.append(" AS ");
			baseCount.append(this.tablePrefix);
			baseCount.append(SPACE);
		}
		
		if(this.baseSelect.joins != null){
			baseCount.append(this.baseSelect.joins);
			baseCount.append(SPACE);
		}
		
		if(this.baseSelect.appliedWhere)
			baseCount.append(this.baseSelect.selectWhere);		
		
		return baseCount.toString();
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#getSelectArgs()
	 */
	@Override
	public List<Object> getSelectArgs() {
		List<Object> args = new ArrayList<Object>(super.getSelectArgs());
		args.addAll(this.baseSelect.getSelectArgs());
		return args;
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#getCountArgs()
	 */
	@Override
	public List<Object> getCountArgs() {
		List<Object> args = new ArrayList<Object>(super.getCountArgs());
		args.addAll(this.baseSelect.getCountArgs());
		return args;
	}
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#appendColumnQuery(com.infiniteautomation.mango.db.query.SQLQueryColumn, java.util.List, com.infiniteautomation.mango.db.query.ComparisonEnum)
	 */
	@Override
	public void appendColumnQuery(SQLQueryColumn column, List<Object> columnArgs, ComparisonEnum comparison) {
		if(column.getName().startsWith(tablePrefix)){
			//Apply to sub-select
			super.appendColumnQuery(column, columnArgs, comparison);
		}else{
			this.baseSelect.appendColumnQuery(column, columnArgs, comparison);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#appendSQL(java.lang.String, java.util.List)
	 */
	@Override
	public void appendSQL(String sql, List<Object> args) {
		super.appendSQL(sql, args);
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#applySort(com.infiniteautomation.mango.db.query.SQLQueryColumn, boolean)
	 */
	@Override
	public void applySort(SQLQueryColumn column, boolean desc) {
		//Determine which statement to apply the sort to
		if(column.getName().startsWith(tablePrefix)){
			//Apply to sub-select
			super.applySort(column, desc);
		}else{
			this.baseSelect.applySort(column, desc);
		}
	}
}
