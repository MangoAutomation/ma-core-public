/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

/**
 * @author Terry Packer
 *
 */
public interface StreamableRowCallback<T> {
	
	public abstract void row(T row, int index) throws Exception; 
	
}
