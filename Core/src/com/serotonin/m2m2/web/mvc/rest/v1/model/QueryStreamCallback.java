/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;

/**
 * @author Terry Packer
 *
 */
public class QueryStreamCallback<T> implements MappedRowCallback<T>{

	private final Log LOG = LogFactory.getLog(QueryStreamCallback.class);
	
	protected JsonGenerator jgen;
	protected CSVPojoWriter<T> csvWriter;
	
	public void setJsonGenerator(JsonGenerator jgen){
		this.jgen = jgen;
	}
	
	public void setCsvWriter(CSVPojoWriter<T> csvWriter){
		this.csvWriter = csvWriter;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.db.MappedRowCallback#row(java.lang.Object, int)
	 */
	@Override
	public void row(T vo, int index) {
		try {
			this.write(vo);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		
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
