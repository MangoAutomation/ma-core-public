/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;

/**
 * 
 * Used to allow the Jackson Mapper to wrap this 
 * entity with a count and results
 * 
 * @author Terry Packer
 *
 */
public interface QueryDataPageStream<T> extends QueryArrayStream<T>{

	/**
	 * Stream the Query Count Value
	 * @param jgen
	 * @throws IOException
	 */
	public void streamCount(JsonGenerator jgen) throws IOException;
	
	public void streamCount(CSVPojoWriter<T> writer) throws IOException;
}
