/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.List;

/**
 * @author Terry Packer
 *
 */
public class SQLLimitOffset implements SQLConstants{
	
	protected List<Object> args;
	
	public SQLLimitOffset(List<Object> args){
		this.args = args;
	}
	
	public List<Object> getArgs(){
		return this.args;
	}
	
	/**
	 * Apply the SQL to the builder
	 * @param builder
	 */
	public void apply(StringBuilder builder){
		builder.append(LIMIT_OFFSET_SQL);
	}
	
}
