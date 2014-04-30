/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao.nosql;

import com.serotonin.m2m2.view.stats.ITime;

/**
 * @author Terry Packer
 *
 */
public interface NoSQLQueryCallback {
	
	public abstract void entry(String storeName, long timestamp, ITime entry);

}
