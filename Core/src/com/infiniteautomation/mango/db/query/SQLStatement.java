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
	
	private static final String SPACE = " ";
	private static final String WHERE = "WHERE ";
	private static final String LIMIT_SQL = "LIMIT ?";
    private static final String OFFSET_SQL = "OFFSET ?";
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
	private boolean applyLimit; //Do we even want to apply it to the generated SQL?
	
	private List<SortOption> sort;
	
	/**
	 * 
	 * @param baseSelectStatement
	 * @param baseCountStatement
	 * @param applyLimitToSelectSql - Do we want the limit/offset applied to the generated SQL?
	 */
	public SQLStatement(String baseSelectStatement, String baseCountStatement, boolean applyLimitToSelectSql){
		this(baseSelectStatement, new ArrayList<Object>(), baseCountStatement, applyLimitToSelectSql);
	}
	
	/**
	 * 
	 * @param baseSelectStatement
	 * @param selectArgs
	 * @param baseCountStatement
	 * @param applyLimitToSelectSql - Do we want the limit/offset applied to the generated SQL?
	 */
	public SQLStatement(String baseSelectStatement, 
			List<Object> selectArgs, String baseCountStatement, boolean applyLimitToSelectSql){
		
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
		this.applyLimit = applyLimitToSelectSql;
		
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
		if(this.applyLimit)
			builder.append(limitOffset.toString());
		
		return builder.toString();
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
