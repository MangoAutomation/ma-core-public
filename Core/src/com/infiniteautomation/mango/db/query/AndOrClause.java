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
public class AndOrClause {
	private AndOrClause parent;
	private ComparisonEnum comparison;
	private List<QueryRestriction> restrictions;
	private List<AndOrClause> children;
	
	public AndOrClause(AndOrClause parent, ComparisonEnum comparison) {
		this.parent = parent;
		this.comparison = comparison;
		this.restrictions = new ArrayList<QueryRestriction>();
		this.children = new ArrayList<AndOrClause>();
	}

	public void addRestriction(QueryRestriction component) {
		this.restrictions.add(component);
	}

	public ComparisonEnum getComparison() {
		return comparison;
	}

	public List<QueryRestriction> getRestrictions() {
		return restrictions;
	}
	
	public List<AndOrClause> getChildren(){
		return children;
	}

	public AndOrClause addChild(ComparisonEnum comparison){
		AndOrClause child = new AndOrClause(this, comparison);
		this.children.add(child);
		return child;
	}
	
	public void addChild(AndOrClause child){
		child.parent = this;
		this.children.add(child);
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

	/**
	 * Are any of our restrictions from a Joined Table (columns that don't start with the prefix)
	 * @return
	 */
	public boolean hasAnyJoinedRestrictions(String tablePrefix) {
		for(QueryRestriction restriction : this.restrictions)
			if(!restriction.column.getName().startsWith(tablePrefix))
				return true;

		for(AndOrClause child : this.children){
			if(child.hasAnyJoinedRestrictions(tablePrefix))
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
