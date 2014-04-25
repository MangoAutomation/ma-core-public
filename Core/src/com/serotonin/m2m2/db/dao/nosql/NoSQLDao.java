/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao.nosql;

import java.util.List;

import com.serotonin.m2m2.view.stats.ITime;

/**
 * 
 * Generic NoSQL Dao to allow Modules to use the NoSQL Datastore
 * 
 * @author Terry Packer
 *
 */
public abstract class NoSQLDao {
	
	protected NoSQLDataSerializer serializer;
	
	public NoSQLDao(NoSQLDataSerializer serializer2){
		this.serializer = serializer2;
	}
	
	
	/**
	 * Store data 
	 * @param storeName
	 * @param data
	 */
	public abstract void storeData(String storeName, List<ITime> data);

	/**
	 * Store data 
	 * @param storeName
	 * @param data
	 */
	public abstract void storeData(String storeName, ITime data);
	
	/**
	 * Query one data store
	 * @param storeName
	 * @param from
	 * @param to
	 * @param callback
	 */
	public abstract void getData(String storeName, long from, long to, NoSQLQueryCallback callback);
	
	/**
	 * Query multiple data stores
	 * @param storeNames
	 * @param from
	 * @param to
	 * @param callback
	 */
	public abstract void getData(List<String> storeNames, long from, long to, NoSQLQueryCallback callback);
	
}
