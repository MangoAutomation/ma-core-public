/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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

	private String subSelectJoins;
	private WhereClause subSelectWhere;
	
	/**
	 * @param baseSelectStatement - SELECT x,y,z
	 * @param selectArgs
	 * @param baseCountStatement
	 * @param applyLimitToSelectSql
	 */
	public SQLSubQuery(String baseSelect, String baseCountStatement, String baseJoins,
			String tableName,
			String tablePrefix,
			boolean applyLimitToSelectSql, String subSelectJoins) {
		super(baseSelect, baseCountStatement, baseJoins, tableName, tablePrefix, applyLimitToSelectSql);
		
		this.subSelectJoins = subSelectJoins;
		this.subSelectWhere = new WhereClause(applyLimitToSelectSql);
	}

	@Override
	public void build(){
		
		//First remove any invalid clauses and place into outer where
		this.pruneSubQuery();
		
		//SELECT tbl.id,tbl.xid, ... FROM
		StringBuilder selectSql = new StringBuilder(this.baseSelect);
		//COUNT ... FROM
		StringBuilder countSql = new StringBuilder(this.baseCount);
		
		if(this.subSelectWhere.hasRestrictions() || this.subSelectWhere.hasLimitOrder()){
			//baseSelect (SELECT * FROM tbl as tbl.prefix WHERE ... )
			selectSql.append("(SELECT * FROM ");
			selectSql.append(this.tableName);
			selectSql.append(SPACE);
			countSql.append(" (SELECT * FROM ");
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
			//Append Joins
			if(this.subSelectJoins != null){
				selectSql.append(SPACE);
				selectSql.append(this.subSelectJoins);
				selectSql.append(SPACE);
				countSql.append(SPACE);
				countSql.append(this.subSelectJoins);
				countSql.append(SPACE);
			}
			//We may only have order/limit
			if(this.subSelectWhere.hasRestrictions()){
				selectSql.append(WHERE);
				countSql.append(WHERE);
			}
			this.subSelectWhere.build();
			selectSql.append(this.subSelectWhere.selectSQL);
			countSql.append(this.subSelectWhere.countSQL);
			selectSql.append(") ");
			countSql.append(") ");
		}else{
			selectSql.append(this.tableName);
			selectSql.append(SPACE);
			countSql.append(this.tableName);
			countSql.append(SPACE);
		}
		
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
		this.selectArgs = new ArrayList<Object>(this.subSelectWhere.selectArgs);
		this.selectArgs.addAll(this.baseWhere.selectArgs);
		
		this.countSQL = countSql.toString();
		this.countArgs = new ArrayList<Object>(this.subSelectWhere.countArgs);
		this.countArgs.addAll(this.baseWhere.countArgs);
	}
	
	@Override
	public void openAndOr(ComparisonEnum comparison){
		if(!this.baseWhere.isOpen())
			this.baseWhere.openNewClause(comparison);
		this.subSelectWhere.openNewClause(comparison);
	}
	
	@Override
	public void closeAndOr(ComparisonEnum comparison){
		this.subSelectWhere.closeCurrentClause();
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#appendColumnQuery(com.infiniteautomation.mango.db.query.SQLQueryColumn, java.util.List, com.infiniteautomation.mango.db.query.ComparisonEnum)
	 */
	@Override
	public void appendColumnQuery(SQLQueryColumn column, List<Object> columnArgs, ComparisonEnum comparison) {
		this.subSelectWhere.addRestrictionToCurrentClause(new QueryRestriction(column, columnArgs, comparison));
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#applySort(com.infiniteautomation.mango.db.query.SQLQueryColumn, boolean)
	 */
	@Override
	public void applySort(SQLQueryColumn column, boolean desc) {
		this.subSelectWhere.addSort(column, desc);
	}

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#applyLimit(java.util.List)
	 */
	@Override
	public void applyLimit(List<Object> args) throws RQLToSQLParseException {
		this.subSelectWhere.applyLimit(args);
	}
	
	@Override
	public List<Object> getLimitOffsetArgs(){
		if(!this.subSelectWhere.hasRestrictions()){
			if(this.baseWhere.limitOffset != null)
				return this.baseWhere.limitOffset.getArgs();
			else
				return null;
		}else{
			if(this.subSelectWhere.limitOffset != null)
				return this.subSelectWhere.limitOffset.getArgs();
			else
				return null;
		}
	}
	
	public void pruneSubQuery(){
		
		//Is there a Where at all?
		if(this.subSelectWhere.getCurrentClause() != null){
		
			//Move to root of tree
			this.subSelectWhere.root();
	
			//Recursively prune it
			if(recursivelyPrune(this.subSelectWhere.getCurrentClause(), this.baseWhere.getCurrentClause()))
				this.subSelectWhere.setCurrentClause(null);
			
			//Move sort
			if(!this.subSelectWhere.hasRestrictions()){
				//Move everything
				ListIterator<SortOption> it = this.subSelectWhere.sort.listIterator();
				while(it.hasNext()){
					this.baseWhere.sort.add(it.next());
					it.remove();
				}
			}else{
				//Only move the items that can't be sorted on the inner
				ListIterator<SortOption> it = this.subSelectWhere.sort.listIterator();
				while(it.hasNext()){
					SortOption option = it.next();
					if(!option.attribute.startsWith(tablePrefix)){
						this.baseWhere.sort.add(option);
						it.remove();
					}
				}
			}
		}else if((this.subSelectWhere.getSingleRestriction() != null)&&(!this.subSelectWhere.getSingleRestriction().column.getName().startsWith(tablePrefix))){
			//Special handling for single restriction
			this.baseWhere.setSingleRestriction(this.subSelectWhere.getSingleRestriction());
			this.subSelectWhere.setSingleRestriction(null);
		}
			
		//Move limits & order if there are not any inner where restrictions
		if(!this.subSelectWhere.hasRestrictions()){
			this.baseWhere.limitOffset = this.subSelectWhere.limitOffset;
			this.subSelectWhere.limitOffset = null;
			ListIterator<SortOption> it = this.subSelectWhere.sort.listIterator();
			while(it.hasNext()){
				SortOption option = it.next();
				this.baseWhere.sort.add(option);
				it.remove();
			}
		}
		
	}
	
	boolean recursivelyPrune(AndOrClause inner, AndOrClause outer){
		
		if((inner.getComparison() == ComparisonEnum.OR)&&(inner.hasAnyJoinedRestrictions(tablePrefix))){
			//Add this entire clause to the outer
			outer.addChild(inner);
			return true; //Mark for removal @ parent
		}else{
			//We are an AND, add any restrictions from the Joined table
			ListIterator<QueryRestriction> it = inner.getRestrictions().listIterator();
			while(it.hasNext()){
				QueryRestriction restriction = it.next();
				if(!restriction.column.getName().startsWith(tablePrefix)){
					//TODO Can this ever even happen? Ensure we are within an AND if we are going to add some restrictions
					if(outer.getComparison() != ComparisonEnum.AND)
						outer = outer.addChild(ComparisonEnum.AND);
					outer.addRestriction(restriction);
					it.remove();
				}
			}
			//Check our children
			ListIterator<AndOrClause> aoIt = inner.getChildren().listIterator();
			while(aoIt.hasNext()){
				if(recursivelyPrune(aoIt.next(), outer))
					aoIt.remove();
			}
		}
		return false;
	}
}
