/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.emport;

import com.serotonin.m2m2.emport.SpreadsheetEmporter.CellType;

/**
 * @author Terry Packer
 *
 */
public class CellTypeValue {

	private String memberName;
	private SpreadsheetEmporter.CellType type;
	private Object value;
	
	
	/**
	 * @param columnName 
	 * @param string
	 * @param readStringCell
	 */
	public void setTypeValue(String memberName, CellType type, Object value) {
		this.memberName = memberName;
		this.type = type;
		this.value = value;
	}


	public String getMemberName() {
		return memberName;
	}


	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}


	public SpreadsheetEmporter.CellType getType() {
		return type;
	}


	public void setType(SpreadsheetEmporter.CellType type) {
		this.type = type;
	}


	public Object getValue() {
		return value;
	}


	public void setValue(Object value) {
		this.value = value;
	}

	
	
	
	
	
	
}
