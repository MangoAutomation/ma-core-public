/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;

/**
 * @author Terry Packer
 *
 */
public class PointValueIdTime extends PointValueTime{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long pointValueId; //Id of row in DB
	
    public PointValueIdTime(int id, DataValue value, long time) {
        super(value,time);
        this.pointValueId = id;
    }

    public PointValueIdTime(int id, boolean value, long time) {
        this(id,new BinaryValue(value), time);
    }

    public PointValueIdTime(int id, int value, long time) {
        this(id,new MultistateValue(value), time);
    }

    public PointValueIdTime(int id, double value, long time) {
        this(id,new NumericValue(value), time);
    }

    public PointValueIdTime(int id, String value, long time) {
        this(id,new AlphanumericValue(value), time);
    }

	public long getPointValueId() {
		return pointValueId;
	}

	public void setPointValueId(long pointValueId) {
		this.pointValueId = pointValueId;
	}
    
    
    
	
	
}
