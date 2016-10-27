/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terry Packer
 *
 */
public class SQLStatement implements SQLConstants{

	protected String baseSelect;
	protected String baseCount;
	protected String baseJoins;
	protected String tableName;
	protected String tablePrefix;

	protected WhereClause baseWhere;
	
	protected String selectSQL;
	protected List<Object> selectArgs;
	
	protected String countSQL;
	protected List<Object> countArgs;
	
	public SQLStatement(String baseSelect, String baseCountStatement, String joins,
			String tableName, String tablePrefix, boolean applyLimitToSelectSql){
		this.baseSelect = baseSelect;
		this.baseCount = baseCountStatement;
		this.baseJoins = joins;
		this.tableName = tableName;
		this.tablePrefix = tablePrefix;
		this.baseWhere = new WhereClause(applyLimitToSelectSql);
	}
	
	/**
	 * Call after build to get full SQL
	 * @return
	 */
	public String getSelectSql() {
		return this.selectSQL;
	}
	
	/**
	 * Call after build to get full SQL
	 * @return
	 */
	public String getCountSql() {
		return this.countSQL;
	}
	
	/**
	 * Call after build to get select arguments
	 * @return
	 */
	public List<Object> getSelectArgs() {
		return this.selectArgs;
	}

	/**
	 * Call after build to get count arguments
	 * @return
	 */
	public List<Object> getCountArgs() {
		return this.countArgs;
	}

	public List<Object> getLimitOffsetArgs(){
		if(this.baseWhere.limitOffset != null)
			return this.baseWhere.limitOffset.getArgs();
		else
			return null;
	}
	
	/**
	 * Add a restriction to the current clause
	 * @param column
	 * @param columnArgs
	 * @param comparison
	 */
	public void appendColumnQuery(SQLQueryColumn column, List<Object> columnArgs, ComparisonEnum comparison) {
		this.baseWhere.addRestrictionToCurrentClause(new QueryRestriction(column, columnArgs, comparison));
	}
	
	/**
	 * Apply Sort
	 * @param column
	 * @param desc
	 */
	public void applySort(SQLQueryColumn column, boolean desc) {
		this.baseWhere.addSort(column, desc);
	}
	
	/**
	 * Apply a limit
	 * @param args
	 * @throws RQLToSQLParseException
	 */
	public void applyLimit(List<Object> args) throws RQLToSQLParseException {
		this.baseWhere.applyLimit(args);
	}
	
//	/* (non-Javadoc)
//	 * @see com.infiniteautomation.mango.db.query.SQLStatement#appendSQL(java.lang.String, java.util.List)
//	 */
//	@Override
//	public void appendSQL(String sql, List<Object> args) {
//		super.appendSQL(sql, args);
//	}
	
	/**
	 * Open a clause with a comparison type
	 * @param comparison
	 */
	public void openAndOr(ComparisonEnum comparison){
		this.baseWhere.openNewClause(comparison);
	}
	
	/**
	 * Close a clause
	 * @param comparison
	 */
	public void closeAndOr(ComparisonEnum comparison){
		this.baseWhere.closeCurrentClause();
	}
	
	/**
	 * Build the Count and Select Statements
	 */
	public void build(){
		//SELECT tbl.id,tbl.xid, ... FROM
		StringBuilder selectSql = new StringBuilder(this.baseSelect);
		//COUNT ... FROM
		StringBuilder countSql = new StringBuilder(this.baseCount);
		
		selectSql.append(this.tableName);
		selectSql.append(SPACE);
		countSql.append(this.tableName);
		countSql.append(SPACE);

		if(this.tablePrefix != null){
			selectSql.append(" AS ");
			selectSql.append(this.tablePrefix);
			selectSql.append(SPACE);
			countSql.append(" AS ");
			countSql.append(this.tablePrefix);
			countSql.append(SPACE);
		}

		if(this.baseJoins != null){
			selectSql.append(SPACE);
			selectSql.append(this.baseJoins);
			selectSql.append(SPACE);
			countSql.append(SPACE);
			countSql.append(this.baseJoins);
			countSql.append(SPACE);
		}
		
		
		//Build up the where clauses
		this.baseWhere.build();
		if(this.baseWhere.hasRestrictions()){
			selectSql.append(WHERE);
			countSql.append(WHERE);
			selectSql.append(this.baseWhere.selectSQL);
			countSql.append(this.baseWhere.countSQL);
		}else if(this.baseWhere.hasLimitOrder()){
			selectSql.append(this.baseWhere.selectSQL);
			countSql.append(this.baseWhere.countSQL);
		}
		
		this.selectSQL = selectSql.toString();
		this.selectArgs = new ArrayList<Object>(this.baseWhere.selectArgs);
		
		this.countSQL = countSql.toString();
		this.countArgs = new ArrayList<Object>(this.baseWhere.countArgs);
	}

}
