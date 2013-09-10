/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

/**
 * @author Terry Packer
 *
 */
public interface IFilter<VO> {
	
	/**
	 * Return true if filter doesn't match (ie. remove the object from a list)
	 * @param vo - the object to use for comparison
	 * @param matches - The object to compare to
	 * @return
	 */
	public boolean filter(VO vo);
	
	public void setFilter(Object matches);
	
}
