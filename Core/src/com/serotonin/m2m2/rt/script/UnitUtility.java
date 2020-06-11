/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import javax.measure.unit.Unit;

import com.serotonin.m2m2.util.JUnitUtil;

/**
 * @author Terry Packer
 *
 */
public class UnitUtility {

	public static final String CONTEXT_KEY = "UnitConverter";
	/**
	 * 
	 * @param base
	 * @param convert
	 * @param value
	 * @return
	 */
	public double convertUnit(Unit<?> base, Unit<?> convert, double value){
		return base.getConverterTo(convert).convert(value);
	}

	/**
	 * Convert value using unit symbols
	 * @param baseSymbol
	 * @param convertSymbol
	 * @param value
	 * @return
	 */
	public double convertUnitSymbol(String baseSymbol, String convertSymbol, double value){
		return this.convertUnit(JUnitUtil.parseLocal(baseSymbol), JUnitUtil.parseLocal(convertSymbol), value);
	}
	
	
	public String getHelp(){
		return toString();
	}
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		builder.append("convertUnit(baseUnit, convertToUnit, value): Number, \n");
		builder.append("convertUnitSymbol(baseUnitSymbol, convertUnitSymbol, value): Number, \n");
		builder.append(" }\n");
		return builder.toString();
	}
	
}
