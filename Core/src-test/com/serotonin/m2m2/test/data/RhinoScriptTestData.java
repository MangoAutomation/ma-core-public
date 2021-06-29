/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.test.data;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;

/**
 * @author Terry Packer
 *
 */
public class RhinoScriptTestData {

	public static long pointValueCount = 100;
	public static long periodMs = 1000; //Poll period for generated data
	public static int pointCount = 3; //Number of points to use
	public static List<List<PointValueTime>> numericPvts;
	
	static {
		long currentTime = Common.timer.currentTimeMillis();
		long relativeTime;
		numericPvts = new ArrayList<List<PointValueTime>>();
		for(int i=0; i<pointCount; i++){
			List<PointValueTime> pvts = new ArrayList<PointValueTime>();
			relativeTime = currentTime - (periodMs * pointValueCount);
			for(int j=0; j<pointValueCount; j++){
				pvts.add(new PointValueTime(new NumericValue(j),relativeTime));
				relativeTime += periodMs; //Step forwards
			}
			numericPvts.add(pvts);
		}
		
	}
	
	
	
	
	/**
	 * @param dataTypeId
	 * @param index
	 * @param limit
	 * @return 
	 */
	public static List<PointValueTime> getLatestPointValues(int dataTypeId, int id, int limit) {
		if(limit > pointValueCount)
			throw new ShouldNeverHappenException("Not Enough Data!");
		if((id <= 0 )||(id > pointCount))
			throw new ShouldNeverHappenException("Invalid Data Point ID!");
		
		switch(dataTypeId){
			case DataTypes.ALPHANUMERIC:
			case DataTypes.BINARY:
			case DataTypes.IMAGE:
			case DataTypes.MULTISTATE:
			default:
				throw new ShouldNeverHappenException("Unimplemented");
			case DataTypes.NUMERIC:
				return numericPvts.get(id-1).subList(numericPvts.get(id-1).size()-limit, numericPvts.get(id-1).size());
		}
		
	}




	/**
	 * @param dataTypeId
	 * @param id
	 * @return
	 */
	public static PointValueTime getLatestPointValue(int dataTypeId, int id) {
		List<PointValueTime> pvts = getLatestPointValues(dataTypeId, id, 1);
		if(pvts.size() > 0)
			return pvts.get(0);
		else
			return null;
	}




	/**
	 * @param dataTypeId
	 * @param from
	 * @param to
	 */
	public static List<PointValueTime> getPointValuesBetween(int dataTypeId, int id, long from, long to) {
		switch(dataTypeId){
		case DataTypes.ALPHANUMERIC:
		case DataTypes.BINARY:
		case DataTypes.IMAGE:
		case DataTypes.MULTISTATE:
		default:
			throw new ShouldNeverHappenException("Unimplemented");
		case DataTypes.NUMERIC:
			List<PointValueTime> values = new ArrayList<PointValueTime>();
			List<PointValueTime> pvts = numericPvts.get(id);
			for(PointValueTime pvt : pvts){
				if((pvt.getTime() > from)||(pvt.getTime() <= to))
					values.add(pvt);
			}
			return values;
		}
		
	}

}
