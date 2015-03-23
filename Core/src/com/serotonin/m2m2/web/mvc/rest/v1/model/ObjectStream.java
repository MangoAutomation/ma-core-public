/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;

/**
 * Used to notify the JacksonObject mapper 
 * that we are a serialized object
 * 
 * @author Terry Packer
 *
 */
public interface ObjectStream<T> {
	
	/**
	 * Stream the data
	 * @param jgen
	 * @throws IOException
	 */
	public void streamData(JsonGenerator jgen) throws IOException;

	public void streamData(CSVPojoWriter<T> jgen) throws IOException;

}
