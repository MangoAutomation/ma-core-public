/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import com.serotonin.m2m2.Common;

/**
 * Registry to access Daos from a central location
 * 
 * Useful for many things, among them Mocking Daos for test
 * 
 * 
 * @author Terry Packer
 *
 */
public class DaoRegistry {

	private DaoRegistry(){
	}
	
	//TODO Maybe set this up to get registered at startup instead of here
	public static DataSourceDao dataSourceDao = new DataSourceDao();
	public static DataPointDao dataPointDao = new DataPointDao();
	public static UserDao userDao = new UserDao();
	public static PointValueDao pointValueDao = Common.databaseProxy.newPointValueDao();
	
}
