/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVEntity;

/**
 * @author Terry Packer
 *
 */
@CSVEntity
public class RestErrorModel extends AbstractRestModel<Exception>{

	private static final String NEWLINE = "\n";
	/**
	 * @param data
	 */
	public RestErrorModel(Exception data) {
		super(data);
	}
	
	public RestErrorModel(){
		super(new Exception());
	}
	@JsonGetter
	@CSVColumnGetter(order = 0, header="message")
	public String getMessage(){
		return this.data.getMessage();
	}
	@JsonSetter
	@CSVColumnSetter(order=0, header="message")
	public void setMessage(){ } //Read only
	
	@JsonGetter
	@CSVColumnGetter(order=1, header="stackTrace")
	public String getStackTrace(){
		StringBuilder trace = new StringBuilder();
		for(StackTraceElement element : data.getStackTrace()){
			trace.append(element.toString() + NEWLINE);
		}
		return trace.toString();
	}
	@JsonSetter
	@CSVColumnSetter(order=1, header="stackTrace")
	public void setStackTrace(){ }
}
