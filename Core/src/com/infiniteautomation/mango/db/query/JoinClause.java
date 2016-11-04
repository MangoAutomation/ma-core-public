/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

/**
 * @author Terry Packer
 *
 */
public class JoinClause implements SQLConstants{
	
	private String join;
	private String tableName;
	private String tablePrefix;
	private String joinOn;
	
	/**
	 * @param join
	 * @param tableName
	 * @param tablePrefix
	 * @param joinOn
	 */
	public JoinClause(String join, String tableName, String tablePrefix, String joinOn) {
		this.join = join;
		this.tableName = tableName;
		this.tablePrefix = tablePrefix;
		this.joinOn = joinOn;
	}
	/**
	 * @return the join
	 */
	public String getJoin() {
		return join;
	}
	/**
	 * @param join the join to set
	 */
	public void setJoin(String join) {
		this.join = join;
	}
	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}
	/**
	 * @param tableName the tableName to set
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	/**
	 * @return the tablePrefix
	 */
	public String getTablePrefix() {
		return tablePrefix;
	}
	/**
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
	/**
	 * @return the joinOn
	 */
	public String getJoinOn() {
		return joinOn;
	}
	/**
	 * @param joinOn the joinOn to set
	 */
	public void setJoinOn(String joinOn) {
		this.joinOn = joinOn;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(SPACE);
		b.append(join);
		b.append(SPACE);
		b.append(tableName);
		b.append(SPACE);
		b.append(tablePrefix);
		b.append(ON);
		b.append(joinOn);
		b.append(SPACE);
		
		return b.toString();
	}

}
