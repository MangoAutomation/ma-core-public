/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;

/**
 * Implement this class to stream an object out using a JsonGenerator
 * 
 * @author Jared Wiltshire
 */
public interface JsonStream<T> {
	
	/**
	 * Stream the data
	 * @param jgen
	 * @throws IOException
	 */
	public void streamData(JsonGenerator jgen) throws IOException;

	public void streamData(CSVPojoWriter<T> jgen) throws IOException;

}
