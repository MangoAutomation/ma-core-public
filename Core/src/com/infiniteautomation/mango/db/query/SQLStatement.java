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
public class SQLStatement {
	
	private static final String SPACE = " ";
	private static final String WHERE = "WHERE ";
	private static final String LIMIT_SQL = "LIMIT ?";
	private static final String LIMIT_OFFSET_SQL = "LIMIT ? OFFSET ?";
	private static final String ORDER_BY = "ORDER BY ";
	private static final String ASC = " ASC ";
	private static final String DESC = " DESC ";
	private static final String COMMA = ",";

	private StringBuilder selectSql;
	private List<Object> selectArgs;

	private StringBuilder countSql;
	
	private boolean appliedWhere;
	
	private StringBuilder limitOffset;
	private List<Object> limitArgs;
	private boolean appliedLimit; //Only can happen once
	
	private List<SortOption> sort;
	
	
	public SQLStatement(String baseSelectStatement, String baseCountStatement){
		this(baseSelectStatement, new ArrayList<Object>(), baseCountStatement);
	}
	
	public SQLStatement(String baseSelectStatement, List<Object> selectArgs, String baseCountStatement){
		
		this.selectSql = new StringBuilder(baseSelectStatement);
		if(!baseSelectStatement.endsWith(SPACE))
			this.selectSql.append(SPACE);
		
		this.countSql = new StringBuilder(baseCountStatement);
		if(!baseCountStatement.endsWith(SPACE))
			this.countSql.append(SPACE);
		
		this.selectArgs = selectArgs;
		this.appliedWhere = false;
		
		this.limitOffset = new StringBuilder();
		this.limitArgs = new ArrayList<Object>();
		this.appliedLimit = false;
		
		this.sort = new ArrayList<SortOption>();
		
	}
	
	public String getSelectSql() {
		StringBuilder builder = new StringBuilder(this.selectSql.toString());
		
		//Apply the sort
		if(this.sort.size() > 0){
			builder.append(ORDER_BY);
			int cnt = 0;
			for(SortOption option : this.sort){
				builder.append(option.attribute);
				if(option.desc)
					builder.append(DESC);
				else
					builder.append(ASC);
				//Append comma?
				if(cnt < this.sort.size() - 1)
					builder.append(COMMA);
				cnt++;
			}
		}
		
		//Apply the limit
		builder.append(limitOffset.toString());
		
		return builder.toString();
	}

	public List<Object> getSelectArgs() {
		//Merge both select and limit
		List<Object> allArgs = new ArrayList<Object>();
		allArgs.addAll(this.selectArgs);
		allArgs.addAll(this.limitArgs);
		return allArgs;
	}
	
	
	public String getCountSql(){
		return this.countSql.toString();
	}
	
	public List<Object> getCountArgs(){
		return this.selectArgs; //Everything but the limit stuff
	}
	
	/**
	 * Add to the limit
	 * @param stmt
	 */
	public void applyLimit(List<Object> args) throws RQLToSQLParseException{
		if(appliedLimit)
			throw new RQLToSQLParseException("Limit cannot be applied twice to a statement");
		
		if(args.size() > 1){
			//Limit, Offset
			this.limitOffset.append(LIMIT_OFFSET_SQL);
		}else{
			//Simple Limit
			this.limitOffset.append(LIMIT_SQL);
		}
		this.appliedLimit = true;
		this.limitArgs.addAll(args);
	}

	/**
	 * 
	 * Append this statement with some Sort operation
	 * @param stmt
	 */
	public void applySort(SQLQueryColumn column, boolean desc) {
		this.sort.add(new SortOption(column.getName(), desc));
		
	}
	
	/**
	 * Append to both count and select
	 * 
	 * @param sql
	 * @param args 
	 * @return
	 */
	public void appendSQL(String sql, List<Object> args) {
		if(!appliedWhere){
			this.selectSql.append(WHERE);
			this.countSql.append(WHERE);
			this.appliedWhere = true;
		}
		
		this.selectSql.append(sql);
		this.selectSql.append(SPACE);
		
		this.selectArgs.addAll(args);
		
		this.countSql.append(sql);
		this.countSql.append(SPACE);
	}


	/**
	 * @param queryColumn
	 * @param subList
	 * @param equalTo
	 */
	public void appendColumnQuery(SQLQueryColumn column,
			List<Object> columnArgs, ComparisonEnum comparison) {
		
		if(!appliedWhere){
			this.selectSql.append(WHERE);
			this.countSql.append(WHERE);
			this.appliedWhere = true;
		}
		column.appendSQL(selectSql, countSql, selectArgs, columnArgs, comparison);
	}
	

	
}
