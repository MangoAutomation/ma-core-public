/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
