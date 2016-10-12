/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Terry Packer
 *
 */
public class SQLStatement {
	
	protected static final String SPACE = " ";
	protected static final String WHERE = "WHERE ";
	protected static final String LIMIT_SQL = "LIMIT ?";
	protected static final String OFFSET_SQL = "OFFSET ?";
	protected static final String LIMIT_OFFSET_SQL = "LIMIT ? OFFSET ?";
	protected static final String ORDER_BY = "ORDER BY ";
	protected static final String ASC = " ASC ";
	protected static final String DESC = " DESC ";
	protected static final String COMMA = ",";

	protected StringBuilder selectSql;
	protected StringBuilder joins;
	protected StringBuilder selectWhere;
	
	protected List<Object> selectArgs;

	protected StringBuilder countSql;
	protected StringBuilder countWhere;
	
	protected boolean appliedWhere;
	
	protected StringBuilder limitOffset;
	protected List<Object> limitArgs;
	protected boolean appliedLimit; //Only can happen once
	protected boolean applyLimit; //Do we even want to apply it to the generated SQL?
	
	protected List<SortOption> sort;
	
	
	public SQLStatement(String baseSelectStatement, String baseCountStatement, String joins, boolean applyLimitToSelectSql){
		this(baseSelectStatement, new ArrayList<Object>(), baseCountStatement, joins, applyLimitToSelectSql);
	}
	
	/**
	 * 
	 * @param baseSelectStatement
	 * @param baseCountStatement
	 * @param applyLimitToSelectSql - Do we want the limit/offset applied to the generated SQL?
	 */
	public SQLStatement(String baseSelectStatement, String baseCountStatement, boolean applyLimitToSelectSql){
		this(baseSelectStatement, new ArrayList<Object>(), baseCountStatement, null, applyLimitToSelectSql);
	}
	
	public SQLStatement(String baseSelectStatement,
			List<Object> selectArgs, String baseCountStatement, boolean applyLimitToSelectSql){
		this(baseSelectStatement, selectArgs, baseCountStatement, null, applyLimitToSelectSql);
	}
	
	/**
	 * 
	 * @param baseSelectStatement
	 * @param selectArgs
	 * @param baseCountStatement
	 * @param applyLimitToSelectSql - Do we want the limit/offset applied to the generated SQL?
	 */
	public SQLStatement(String baseSelectStatement,
			List<Object> selectArgs, String baseCountStatement, String joins, boolean applyLimitToSelectSql){
		
		this.selectSql = new StringBuilder(baseSelectStatement);
		if(!baseSelectStatement.endsWith(SPACE))
			this.selectSql.append(SPACE);
		
		if(joins != null)
			this.joins = new StringBuilder(joins);
		
		this.selectWhere = new StringBuilder(WHERE);
		this.countWhere = new StringBuilder(WHERE);
		
		this.countSql = new StringBuilder(baseCountStatement);
		if(!baseCountStatement.endsWith(SPACE))
			this.countSql.append(SPACE);
		
		this.selectArgs = selectArgs;
		this.appliedWhere = false;
		
		this.limitOffset = new StringBuilder();
		this.limitArgs = new ArrayList<Object>();
		this.appliedLimit = false;
		this.applyLimit = applyLimitToSelectSql;
		
		this.sort = new ArrayList<SortOption>();
		
	}
	
	public String getSelectSql() {
		StringBuilder builder = new StringBuilder(this.selectSql.toString());
		
		//Apply any Joins
		if(this.joins != null)
			builder.append(joins);
		
		//Apply the where
		if(this.appliedWhere)
			builder.append(this.selectWhere);
		
		//Apply the sort
		this.applySort(builder);
		
		//Apply the limit
		if(this.applyLimit)
			builder.append(limitOffset.toString());
		
		return builder.toString();
	}

	/**
	 * @param builder
	 */
	protected void applySort(StringBuilder builder) {
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
	}

	public List<Object> getSelectArgs() {
		//Merge both select and limit
		List<Object> allArgs = new ArrayList<Object>();
		allArgs.addAll(this.selectArgs);
		if(this.applyLimit)
			allArgs.addAll(this.limitArgs);
		return allArgs;
	}
	
	
	public String getCountSql(){
		StringBuilder sb = new StringBuilder(this.countSql);
		
		//Apply Joins
		if((this.joins != null) && this.appliedWhere)
			sb.append(joins);
		
		//Apply Where Clause
		if(this.appliedWhere)
			sb.append(this.countWhere);
		return sb.toString();
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
		
		if (args.get(0).equals(Double.POSITIVE_INFINITY)) {
		    if ((args.size() > 1) && (!(args.get(1) instanceof ASTNode))){
		        // apply offset only
		        this.limitOffset.append(OFFSET_SQL);
		        this.limitArgs.add(args.get(1));
		        this.appliedLimit = true;
		    }
		    return;
		}
		
		if ((args.size() > 1) && (!(args.get(1) instanceof ASTNode))){
            //Limit, Offset
            this.limitOffset.append(LIMIT_OFFSET_SQL);
            this.limitArgs.add(args.get(0));
            this.limitArgs.add(args.get(1));
		}else{
			//Simple Limit
			this.limitOffset.append(LIMIT_SQL);
			this.limitArgs.add(args.get(0));
		}

        this.appliedLimit = true;
		
	}

	/**
	 * Get the arguments for the limit (offset)
	 * 
	 * Size 1 - Limit only
	 * Size 2 - Limit and Offset
	 * 
	 * @return
	 */
	public List<Object> getLimitOffsetArgs(){
		return this.limitArgs;
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
		if(!appliedWhere)
			this.appliedWhere = true;
		
		this.selectWhere.append(sql);
		this.selectWhere.append(SPACE);
		
		this.countWhere.append(sql);
		this.countWhere.append(SPACE);
		
		this.selectArgs.addAll(args);
	}


	/**
	 * @param queryColumn
	 * @param subList
	 * @param equalTo
	 */
	public void appendColumnQuery(SQLQueryColumn column,
			List<Object> columnArgs, ComparisonEnum comparison) {
		if(!appliedWhere)
			this.appliedWhere = true;
		column.appendSQL(selectWhere, countWhere, selectArgs, columnArgs, comparison);
	}
}
