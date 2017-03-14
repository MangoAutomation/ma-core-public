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
public interface NoSQLDataSerializer {
	
	/**
	 * Create the object from a byte
	 * @param bytes
	 * @return
	 */
	public ITime getObject(byte[] bytes, int readOffset, int writeOffset, long timestamp, String seriesId);
	
	public byte[] getBytes(ITime entry, long timestamp, String seriesId);
	
    public boolean equals(Object other);
	
    public int hashCode();
}
