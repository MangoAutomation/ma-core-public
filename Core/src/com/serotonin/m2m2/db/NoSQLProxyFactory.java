/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db;

/**
 * @author Terry Packer
 *
 */
public class NoSQLProxyFactory {

	public static NoSQLProxyFactory instance = new NoSQLProxyFactory();
	private NoSQLProxy proxy;
	
	
	private NoSQLProxyFactory(){
		
	}
	
	public void setProxy(NoSQLProxy proxy){
		this.proxy = proxy;
	}
	public NoSQLProxy getProxy(){
		return this.proxy;
	}
	
}
