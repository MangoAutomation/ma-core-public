/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
