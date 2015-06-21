/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.time;

import com.serotonin.ShouldNeverHappenException;

/**
 * @author Terry Packer
 *
 */
public enum RollupEnum {
	NONE(true, 0),
	AVERAGE(false, 1),
	DELTA(false, 2),
	MINIMUM(false, 3), 
	MAXIMUM(false, 4),
    ACCUMULATOR(false, 5),
	SUM(false, 6), 
	FIRST(true, 7), 
	LAST(true, 8), 
	COUNT(true, 9),
	INTEGRAL(false, 10);

	private boolean nonNumericSupport; //Does this rollup support Non-Numeric point values
	private int id;
	
	private RollupEnum(boolean nonNumericSupport, int id){
		this.nonNumericSupport = nonNumericSupport;
		this.id = id;
	}
	public boolean nonNumericSupport(){
		return this.nonNumericSupport;
	}
	public int getId(){
		return this.id;
	}
	
	public static RollupEnum convertTo(int id){
		for(RollupEnum r : RollupEnum.values())
			if(r.id == id)
				return r;
		
		throw new ShouldNeverHappenException("Uknown Rollup, id: " + id);
	}
}
