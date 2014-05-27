/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

/**
 * @author Terry Packer
 *
 */
public class ResultSetCounter{
	private long count;
	private long total;
	
	public ResultSetCounter(){
		count = 0L;
	}
	
	public void increment(){
		count++;
	}
	public long getCount(){
		return count;
	}
	public double getProgress(){
		return ((double)count/(double)total) * 100.0D;
	}
	public void setTotal(long total){
		this.total = total;
	}
	public long getTotal() {
		return total;
	}
	
}
