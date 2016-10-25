/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jazdw.rql.parser.ASTNode;

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
	
	
	private String baseSelect;
	private String baseCount;
	private String baseJoins;
	
	private String selectSQL;
	private String countSQL;
	
	//TODO Merge these into 1 Object of cascading where clauses to support infinite subQueries?
	private WhereClause baseWhere;
	private WhereClause subSelectWhere;
		
	public void openAndOr(ComparisonEnum comparison){
		this.baseWhere.openNewClause(comparison);
		this.subSelectWhere.openNewClause(comparison);
	}
	
	public void closeAndOr(ComparisonEnum comparison){
		this.baseWhere.closeCurrentClause();
		this.subSelectWhere.closeCurrentClause();
	}
	
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
		this.baseSelect = baseSelect;
		this.baseCount = baseCountStatement;
		this.baseJoins = joins;
		this.tableName = tableName;
		this.tablePrefix = tablePrefix;
		this.baseWhere = new WhereClause(applyLimitToSelectSql);
		this.subSelectWhere = new WhereClause(applyLimitToSelectSql);
	}

	public void build(){
		
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
			if(this.joins != null){
				selectSql.append(SPACE);
				selectSql.append(this.joins);
				selectSql.append(SPACE);
				countSql.append(SPACE);
				countSql.append(this.joins);
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
		this.countSQL = countSql.toString();
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#getSelectSql()
	 */
	@Override
	public String getSelectSql() {
		return this.selectSQL;
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#getCountSql()
	 */
	@Override
	public String getCountSql() {
		return this.countSQL;
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#getSelectArgs()
	 */
	@Override
	public List<Object> getSelectArgs() {
		List<Object> args = new ArrayList<Object>(this.subSelectWhere.selectArgs);
		args.addAll(this.baseWhere.selectArgs);
		return args;
	}
	
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#getCountArgs()
	 */
	@Override
	public List<Object> getCountArgs() {
		List<Object> args = new ArrayList<Object>(this.subSelectWhere.countArgs);
		args.addAll(this.baseWhere.countArgs);
		return args;
	}
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#appendColumnQuery(com.infiniteautomation.mango.db.query.SQLQueryColumn, java.util.List, com.infiniteautomation.mango.db.query.ComparisonEnum)
	 */
	@Override
	public void appendColumnQuery(SQLQueryColumn column, List<Object> columnArgs, ComparisonEnum comparison) {
		//If at any time we add a Restriction to the base WHERE 
		// we must move the sub-query current clause into the base WHERE since we can't OR or AND with a JOINed table in the inner where
		if(column.getName().startsWith(tablePrefix)){
			if(this.baseWhere.hasRestrictions()){
				this.baseWhere.mergeClause(this.subSelectWhere.currentClause);
				this.baseWhere.addRestrictionToCurrentClause(new Restriction(column, columnArgs, comparison));
			}else{
				this.subSelectWhere.addRestrictionToCurrentClause(new Restriction(column, columnArgs, comparison));
			}
		}else{
			if(this.subSelectWhere.hasRestrictions()){
				this.baseWhere.mergeClause(this.subSelectWhere.currentClause);
			}
			//Must be from a JOIN, add to outer where
			this.baseWhere.addRestrictionToCurrentClause(new Restriction(column, columnArgs, comparison));
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
		//Expected to happen after all WHERE clauses are done being populated
		if(column.getName().startsWith(tablePrefix)){
			//Will there even be a sub-query? If so we can sort it
			if(this.subSelectWhere.hasRestrictions()){
				this.subSelectWhere.addSort(column, desc);
			}else{
				this.baseWhere.addSort(column, desc);
			}
		}else{
			//Must be from a JOIN, add to outer where
			this.baseWhere.addSort(column, desc);
		}
	}

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.db.query.SQLStatement#applyLimit(java.util.List)
	 */
	@Override
	public void applyLimit(List<Object> args) throws RQLToSQLParseException {
		//Expected to happen after all WHERE clauses are done being populated
		if(this.baseWhere.hasRestrictions()){
			this.baseWhere.applyLimit(args);
		}else{
			this.subSelectWhere.applyLimit(args);
		}
	}
	
	class Restriction{
		
		SQLQueryColumn column;
		List<Object> columnArgs;
		ComparisonEnum comparison;
		
		public Restriction(SQLQueryColumn column, List<Object> columnArgs, ComparisonEnum comparison) {
			super();
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
	
	class AndOrClause{
		
		private AndOrClause parent;
		private ComparisonEnum comparison;
		private List<Restriction> restrictions;
		private List<AndOrClause> children;
		
		public AndOrClause(AndOrClause parent, ComparisonEnum comparison) {
			super();
			this.parent = parent;
			this.comparison = comparison;
			this.restrictions = new ArrayList<Restriction>();
			this.children = new ArrayList<AndOrClause>();
		}

		public void addRestriction(Restriction component) {
			this.restrictions.add(component);
		}

		public ComparisonEnum getComparison() {
			return comparison;
		}

		public List<Restriction> getRestrictions() {
			return restrictions;
		}

		public AndOrClause addChild(ComparisonEnum comparison){
			AndOrClause child = new AndOrClause(this, comparison);
			this.children.add(child);
			return child;
		}
		
		public AndOrClause getParent(){
			return this.parent;
		}
		
		public boolean hasRestrictions(){
			return this.restrictions.size() > 0;
		}
		
		public boolean hasAnyRestrictions(){
			if(this.restrictions.size() > 0)
				return true;
			for(AndOrClause child : this.children){
				if(child.hasAnyRestrictions())
					return true;
			}
			return false;
		}
		
		public String toString(){
			StringBuilder builder = new StringBuilder();
			for(int i=0; i<this.restrictions.size(); i++){
				builder.append(this.restrictions.get(i).toString());
				if(i < this.restrictions.size() - 1)
					builder.append(" " + comparison + " ");
			}
			return builder.toString();
		}
	}
	
	class WhereClause{
		
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
		private Restriction singleRestriction;
		//Tree of clauses
		private AndOrClause currentClause;
		
		public WhereClause(boolean applyLimit){
			this.applyLimit = applyLimit;
			this.selectArgs = new ArrayList<Object>();
			this.countArgs = new ArrayList<Object>();
			this.sort = new ArrayList<SortOption>();
		}
		
		/**
		 * @return
		 */
		public boolean hasLimitOrder() {
			return (this.limitOffset != null) || (this.sort.size() > 0);
		}

		/**
		 * @param args
		 */
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

		/**
		 * Merge the current incoming clause into our current clause
		 * while removing restrictions from the incoming clause
		 */
		public void mergeClause(AndOrClause currentClause) {
			AndOrClause root = currentClause;
			
			while(root != null){
				//Get the restrictions for the clause
				ListIterator<Restriction> it = root.getRestrictions().listIterator();
				while(it.hasNext()){
					this.currentClause.addRestriction(it.next());
					it.remove();
				}
				//Add the children recursiveley to this clause and remove them
				ListIterator<AndOrClause> childIt = root.children.listIterator();
				while(childIt.hasNext()){
					this.currentClause.children.add(childIt.next());
					childIt.remove();
				}
				root = root.parent;
			}
		}

		/**
		 * @param column
		 * @param desc
		 */
		public void addSort(SQLQueryColumn column, boolean desc) {
			this.sort.add(new SortOption(column.getName(), desc));
		}

		public void openNewClause(ComparisonEnum comparison){
			if(this.currentClause == null){
				this.currentClause = new AndOrClause(null, comparison);
				//TODO Decide to add single restriction and clear it here?
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
		
		public void addRestrictionToCurrentClause(Restriction restriction){
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
		
		public boolean hasRestrictions(){
			//Walk up to the parent from the current clause
			AndOrClause root  = this.currentClause;
			if((root == null)&&(this.singleRestriction == null))
				return false;
			else if(this.singleRestriction != null)
				return true;
			
			while(root.parent != null)
				root = root.parent;
			
			return root.hasAnyRestrictions();
		}
		
		
		public void build(){
			if(built)
				return;

			StringBuilder selectSql = new StringBuilder();
			StringBuilder countSql = new StringBuilder();
			
			//Walk up to the parent from the current clause
			AndOrClause root  = this.currentClause;
			if(root != null){
				while(root.parent != null)
					root = root.parent;
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
				selectSql.append(clause.parent.comparison.name());
				countSql.append(clause.parent.comparison.name());
			}
			
			if(clause.hasRestrictions()){
				first.set(false);
				selectSql.append(" ( ");
				countSql.append(" ( ");				
			}
			

			//Apply the restrictions
			int restrictionCount = clause.restrictions.size();
			int i = 0;
			for(Restriction restriction : clause.restrictions){
				restriction.appendSQL(selectSql, countSql, selectArgs, countArgs);
				i++;
				if(i < restrictionCount){
					selectSql.append(SPACE + clause.comparison.name() + SPACE);
					countSql.append(SPACE + clause.comparison.name() + SPACE);
				}
			}
			//Apply the children
			for(AndOrClause child : clause.children){
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
}
