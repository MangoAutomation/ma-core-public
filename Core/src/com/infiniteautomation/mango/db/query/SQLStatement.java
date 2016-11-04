/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy.DatabaseType;

/**
 * @author Terry Packer
 *
 */
public class SQLStatement implements SQLConstants{

	protected String baseSelect;
	protected String baseCount;
	protected List<JoinClause> baseJoins;
	protected String tableName;
	protected String tablePrefix;
	protected List<Index> indexes;
	protected DatabaseType databaseType;

	protected WhereClause baseWhere;
	
	protected String selectSQL;
	protected List<Object> selectArgs;
	
	protected String countSQL;
	protected List<Object> countArgs;
	
	public SQLStatement(String baseSelect, String baseCountStatement, List<JoinClause> joins,
			String tableName, String tablePrefix, boolean applyLimitToSelectSql, List<Index> indexes,
			DatabaseType type){
		this.baseSelect = baseSelect;
		this.baseCount = baseCountStatement;
		this.baseJoins = joins;
		this.tableName = tableName;
		this.tablePrefix = tablePrefix;
		this.indexes = indexes;
		this.databaseType = type;
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

		//Check to see if we should force the use of any indexes
		addForceIndexSql(this.baseWhere, this.indexes, this.tablePrefix, selectSql, countSql);
		
		if(this.baseJoins != null)
			addJoinSql(this.baseWhere, this.baseJoins, this.indexes, selectSql, countSql);
		
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


	protected void addForceIndexSql(WhereClause where, List<Index> indexes, String tablePrefix, StringBuilder selectSql,
			StringBuilder countSql) {
		if(this.databaseType != DatabaseType.MYSQL)
			return;
		
		List<Index> toUse = new ArrayList<Index>();
		Map<Index, Integer> maybeUse = new HashMap<Index, Integer>(); //Compound indexes

		//TODO Check the restrictions
		//TODO add to select SQL
		//TODO add to count SQL here but not during sort
		//TODO Clear the list and map
		
		//Check the ORDER BY
		for(SortOption sort : where.sort){
			for(Index index : indexes){
				//only want indexes for the table we are operating on
				if(!index.getTablePrefix().equals(tablePrefix))
					continue;
				for(QueryAttribute column : index.getColumns()){
					if(sort.attribute.startsWith(index.getTablePrefix())){
						//TODO Make this much better with table prefix in sort option
						if((sort.attribute.endsWith(column.getColumnName()) && (sort.desc == index.getType().equals("DESC")))){
							if(index.getColumns().size() == 1)
								toUse.add(index);
							else{
								Integer count = maybeUse.get(index);
								if(count == null){
									maybeUse.put(index, new Integer(1));
								}else{
									//Maybe we can use it
									if(count == index.getColumns().size() - 1){
										//Remove and use
										maybeUse.remove(index);
										toUse.add(index);
									}else{
										maybeUse.put(index, count++);
									}
								}
							}
						}
					}
				}
			}
		}
		
		//Add the SQL
		if(toUse.size() > 0){
			selectSql.append(" FORCE INDEX (");
			int count = 0;
			for(Index index : toUse){
				selectSql.append(index.getName());
				count++;
				if(count < toUse.size()){
					selectSql.append(",");
					countSql.append(",");
				}
			}
			selectSql.append(")");
		}
		
	}

	/**
	 * Generate the JOIN SQL based on the Where clause
	 * 
	 * @param where
	 * @param joins
	 * @return
	 */
	protected void addJoinSql(WhereClause where, List<JoinClause> joins, List<Index> indexes, StringBuilder selectSql, StringBuilder countSql) {
		if(joins.size() == 0)
			return;
		
		for(JoinClause join : joins){
			selectSql.append(SPACE);
			selectSql.append(join.getJoin());
			selectSql.append(SPACE);
			selectSql.append(join.getTableName());
			selectSql.append(SPACE);
			selectSql.append(join.getTablePrefix());
			selectSql.append(SPACE);
			
			//Don't bother with the joins for the count if there are no restrictions on them
			if(where.hasRestrictions()){
				countSql.append(SPACE);
				countSql.append(join.getJoin());
				countSql.append(SPACE);
				countSql.append(join.getTableName());
				countSql.append(SPACE);
				countSql.append(join.getTablePrefix());
				countSql.append(SPACE);
			}
			
			//Mabye force index
			addForceIndexSql(where, indexes, join.getTablePrefix(), selectSql, countSql);
			selectSql.append(ON);
			selectSql.append(join.getJoinOn());
			if(where.hasRestrictions()){
				countSql.append(ON);
				countSql.append(join.getJoinOn());
			}
		}
		
		selectSql.append(SPACE);
		countSql.append(SPACE);
	}
	
}
