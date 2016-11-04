/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The mapping of One Column to POJO Member and any aliases
 * 
 * @author Terry Packer
 *
 */
public class QueryAttribute {

	private String columnName;
	private Set<String> aliases;
	@JsonIgnore
	private int sqlType;
	
	public QueryAttribute(){
		this.aliases = new HashSet<String>();
	}
	
	
	/**
	 * 
	 * @param columnName
	 * @param aliases
	 * @param sqlType
	 */
	public QueryAttribute(String columnName,
			Set<String> aliases, int sqlType) {
		super();
		this.columnName = columnName;
		this.aliases = aliases;
		this.sqlType = sqlType;
	}

	public void addAlias(String alias){
		this.aliases.add(alias);
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public Set<String> getAliases() {
		return aliases;
	}

	public void setAliases(Set<String> aliases) {
		this.aliases = aliases;
	}

	@JsonIgnore
	public int getSqlType() {
		return sqlType;
	}
	@JsonIgnore
	public void setSqlType(int sqlType) {
		this.sqlType = sqlType;
	}
	@JsonGetter("sqlType")
	public String getSqlTypeName() {
		return JdbcTypeNameCodes.instance.getTypeName(sqlType);
	}
	@JsonSetter("sqlType")
	public void setSqlTypeName(String sqlType) {
		this.sqlType = JdbcTypeNameCodes.instance.getType(sqlType);
	}
	
}
