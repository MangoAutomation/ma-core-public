/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terry Packer
 *
 */
public class WidePointValues {
	
	private PointValueTime before;
	private List<PointValueTime> values;
	private PointValueTime after;
	
	public WidePointValues(){
		this.values = new ArrayList<PointValueTime>();
	}
	
	public void addValue(PointValueTime value){
		this.values.add(value);
	}
	
	public PointValueTime getBefore() {
		return before;
	}
	public void setBefore(PointValueTime before) {
		this.before = before;
	}
	public List<PointValueTime> getValues() {
		return values;
	}
	public void setValues(List<PointValueTime> values) {
		this.values = values;
	}
	public PointValueTime getAfter() {
		return after;
	}
	public void setAfter(PointValueTime after) {
		this.after = after;
	}
	
}
