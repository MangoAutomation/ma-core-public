/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import com.serotonin.ShouldNeverHappenException;

/**
 * @author Terry Packer
 *
 */
public enum ComparisonEnum {

	EQUAL_TO,
	NOT_EQUAL_TO,
	LESS_THAN,
	LESS_THAN_EQUAL_TO,
	GREATER_THAN,
	GREATER_THAN_EQUAL_TO,
	IN,
	LIKE,
	NOT_LIKE,
	CONTAINS;
	
	public static ComparisonEnum convertTo(String comparison){
		switch(comparison){
        case "eq":
        	return EQUAL_TO;
        case "gt":
        	return GREATER_THAN;
        case "ge":
        	return GREATER_THAN_EQUAL_TO;
        case "lt":
        	return LESS_THAN;
        case "le":
        	return LESS_THAN_EQUAL_TO;
        case "ne":
        	return NOT_EQUAL_TO;
        case "match":
        case "like":
        	return LIKE;
        case "not like":
        	return NOT_LIKE;
        case "in":
        	return IN;
		}
		throw new ShouldNeverHappenException("Comparison: " + comparison + " not supported.");
	}
	
	
}
