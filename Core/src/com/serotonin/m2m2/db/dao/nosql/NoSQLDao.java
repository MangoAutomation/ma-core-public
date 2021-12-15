/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
     */
	public abstract void storeData(String storeName, List<ITime> data);

	/**
	 * Store data
     */
	public abstract void storeData(String storeName, ITime data);
	

	/**
	 * Query One Data Store
	 * @param limit (-1 for no limit)
	 * @param reverse (read results back in reverse order)
     */
	public abstract void getData(String storeName, long from, long to, int limit, boolean reverse, final NoSQLQueryCallback callback);
	

	/**
	 * Query Multiple data stores
	 * @param limit (-1 for no limit)
	 * @param reverse (read results back in reverse order)
     */
	public abstract void getData(List<String> storeNames, long from, long to, final NoSQLQueryCallback callback);


	/**
	 * Delete an entire data store
	 * 
	 * @param store name
	 */
	public abstract void deleteStore(String storeName);
	
	
	/**
	 * Delete some data within a store
	 * 
	 *  @param store name
     */
	public abstract void deleteData(String storeName, long from, long to);
}
