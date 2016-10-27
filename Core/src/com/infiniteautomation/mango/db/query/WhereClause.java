/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jazdw.rql.parser.ASTNode;

/**
 * Container for a single restriction or a Tree of Clauses with restrictions
 * 
 * @author Terry Packer
 *
 */
public class WhereClause extends SQLConstants{
	
	boolean built; //Have we been built yet
	//In some situations when we filter in memory 
	// it is not possible to limit the query results
	boolean applyLimit;
	String selectSQL;
	List<Object> selectArgs;
	String countSQL;
	List<Object> countArgs;
	List<SortOption> sort;
	SQLLimitOffset limitOffset;
	
	//If there are no clauses
	private QueryRestriction singleRestriction;
	//Tree of clauses
	private AndOrClause currentClause;
	
	public WhereClause(boolean applyLimit){
		this.applyLimit = applyLimit;
		this.selectArgs = new ArrayList<Object>();
		this.countArgs = new ArrayList<Object>();
		this.sort = new ArrayList<SortOption>();
	}
	
	public AndOrClause getCurrentClause(){
		return currentClause;
	}
	
	public void setCurrentClause(AndOrClause clause){
		this.currentClause = clause;
	}

	public boolean isOpen() {
		return this.currentClause != null;
	}
	
	public QueryRestriction getSingleRestriction() {
		return singleRestriction;
	}

	public void setSingleRestriction(QueryRestriction singleRestriction) {
		this.singleRestriction = singleRestriction;
	}

	public boolean hasLimitOrder() {
		return (this.limitOffset != null) || (this.sort.size() > 0);
	}

	public void applyLimit(List<Object> args) {
		if (args.get(0).equals(Double.POSITIVE_INFINITY)) {
		    if ((args.size() > 1) && (!(args.get(1) instanceof ASTNode))){
		        // apply offset only
		    	this.limitOffset = new SQLOffset(args);
		    }
		    return;
		}else{
			if ((args.size() > 1) && (!(args.get(1) instanceof ASTNode))){
	            //Limit, Offset
	            this.limitOffset = new SQLLimitOffset(args);
			}else{
				//Simple Limit
				this.limitOffset = new SQLLimit(args);
			}	
		}
	}

	public void addSort(SQLQueryColumn column, boolean desc) {
		this.sort.add(new SortOption(column.getName(), desc));
	}

	public void openNewClause(ComparisonEnum comparison){
		if(this.currentClause == null){
			this.currentClause = new AndOrClause(null, comparison);
		}else{
			//Open a new clause and set that to the current
			this.currentClause = this.currentClause.addChild(comparison);
		}
	}

	public void closeCurrentClause(){
		//Close clause and go up to parent if there is one
		if(currentClause.getParent() != null)
			this.currentClause = this.currentClause.getParent();
	}
	
	public void addRestrictionToCurrentClause(QueryRestriction restriction){
		if(this.currentClause == null)
			this.singleRestriction = restriction;
		else
			this.currentClause.addRestriction(restriction);
	}

	public boolean currentClauseHasRestriction(){
		if(this.currentClause == null)
			return false;
		else{
			return this.currentClause.hasRestrictions();
		}
	}
	
	/**
	 * Assume we are at root
	 * @return
	 */
	public boolean hasRestrictions(){
		AndOrClause root  = this.currentClause;
		if((root == null)&&(this.singleRestriction == null))
			return false;
		else if(this.singleRestriction != null)
			return true;
		
		return root.hasAnyRestrictions();
	}
	
	/**
	 * Navigate to the root of the tree
	 */
	public void root(){
		if(this.currentClause == null)
			return;
		while(this.currentClause.getParent() != null)
			this.currentClause = this.currentClause.getParent();
	}
	
	/**
	 * Assume we are at root to build
	 */
	public void build(){
		if(built)
			return;

		StringBuilder selectSql = new StringBuilder();
		StringBuilder countSql = new StringBuilder();
		
		//Walk up to the parent from the current clause
		AndOrClause root  = this.currentClause;
		if(root != null){
			//Recursively build the comparisons
			this.recursivelyApply(root, selectSql, this.selectArgs, countSql, this.countArgs, new AtomicBoolean(true));
		}else if(this.singleRestriction != null){
			this.singleRestriction.appendSQL(selectSql, countSql, selectArgs, countArgs);
			selectSql.append(SPACE);
			countSql.append(SPACE);
		}
		
		this.applySort(selectSql);
		if((this.limitOffset != null)&&(this.applyLimit)){
			this.limitOffset.apply(selectSql);
			this.selectArgs.addAll(this.limitOffset.getArgs());
		}
		
		this.selectSQL = selectSql.toString();
		this.countSQL = countSql.toString();
		
		this.built = true;
		
	}
	
	protected void recursivelyApply(AndOrClause clause, 
			StringBuilder selectSql, List<Object> selectArgs, 
			StringBuilder countSql, List<Object> countArgs,
			AtomicBoolean first){
		
		if(!first.get() && clause.hasRestrictions()){
			selectSql.append(clause.getParent().getComparison().name());
			countSql.append(clause.getParent().getComparison().name());
		}
		
		if(clause.hasRestrictions()){
			first.set(false);
			selectSql.append(" ( ");
			countSql.append(" ( ");				
		}

		//Apply the restrictions
		int restrictionCount = clause.getRestrictions().size();
		int i = 0;
		for(QueryRestriction restriction : clause.getRestrictions()){
			restriction.appendSQL(selectSql, countSql, selectArgs, countArgs);
			i++;
			if(i < restrictionCount){
				selectSql.append(SPACE + clause.getComparison().name() + SPACE);
				countSql.append(SPACE + clause.getComparison().name() + SPACE);
			}
		}
		//Apply the children
		for(AndOrClause child : clause.getChildren()){
			recursivelyApply(child, selectSql, selectArgs, countSql, countArgs, first);
		}
		
		if(clause.hasRestrictions()){
			selectSql.append(" ) ");
			countSql.append(" ) ");	
		}
	}
	
	protected void applySort(StringBuilder builder) {
		if(this.sort.size() > 0){
			builder.append(ORDER_BY);
			int cnt = 0;
			for(SortOption option : this.sort){
				//TODO H2 expects an column number NOT a ?=column name
				//builder.append(" ? ");
				//this.selectArgs.add(option.attribute);
				//This is ok so long as we only used real column names via dao.getQueryColumn() sql injection wise
				builder.append(SPACE);
				builder.append(option.attribute);
				builder.append(SPACE);
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
}
