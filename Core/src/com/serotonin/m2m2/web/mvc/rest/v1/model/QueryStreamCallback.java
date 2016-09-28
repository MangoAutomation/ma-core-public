/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.infiniteautomation.mango.db.query.StreamableRowCallback;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;

/**
 * @author Terry Packer
 *
 */
public class QueryStreamCallback<T> implements StreamableRowCallback<T>{

	protected JsonGenerator jgen;
	protected CSVPojoWriter<T> csvWriter;
	
	/**
	 * Set the JsonGenerator, can only be done once 
	 * per instance.
	 * @param jgen
	 */
	public void setJsonGenerator(JsonGenerator jgen){
		if(this.jgen == null)
			this.jgen = jgen;
		else
			throw new ShouldNeverHappenException("Can't re-set JsonGenerator!");
	}
	
	/**
	 * Set the CSV Writer, can only be done once per instance.
	 * @param csvWriter
	 */
	public void setCsvWriter(CSVPojoWriter<T> csvWriter){
		if(this.csvWriter == null)
			this.csvWriter = csvWriter;
		else
			throw new ShouldNeverHappenException("Can't re-set CsvWriter!");
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.db.MappedRowCallback#row(java.lang.Object, int)
	 */
	@Override
	public void row(T vo, int index) throws Exception{
		this.write(vo);
	}
	
	protected void write(T vo) throws IOException{
		if(this.jgen != null)
			this.writeJson(vo);
		else if(this.csvWriter != null)
			this.writeCsv(vo);
	}
	
	/**
	 * Do the work of writing the VO
	 * @param vo
	 * @throws IOException
	 */
	protected void writeJson(T vo) throws IOException{
		this.jgen.writeObject(vo);
	}
	
	protected void writeCsv(T vo) throws IOException{
		this.csvWriter.writeNext(vo);
	}


}
