/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.converters;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoReader;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

/**
 * @author Terry Packer
 *
 */
public class CsvMessageConverter extends AbstractHttpMessageConverter<List<AbstractRestModel<?>>>
{
	  public static final MediaType MEDIA_TYPE = new MediaType("text", "csv");
	  private final char separator, quote;

	  public CsvMessageConverter() {
	    this(CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER);
	  }

	  public CsvMessageConverter(char separator, char quote) {
	    super(MEDIA_TYPE);
	    this.separator = separator;
	    this.quote = quote;
	  }

	  @Override
	  protected boolean supports(Class<?> clazz) {
		  return List.class.isAssignableFrom(clazz);
	  }

	/* (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected List<AbstractRestModel<?>> readInternal(Class<? extends List<AbstractRestModel<?>>> clazz,
			HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		
		
		CSVPojoReader in = new CSVPojoReader(new CSVReader(new InputStreamReader(inputMessage.getBody()),separator, quote));
		List records = in.readAll();
		in.close();
		return records;
	}

	/* (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void writeInternal(List<AbstractRestModel<?>> records, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		if(records.size()>0){
			CSVPojoWriter out = new CSVPojoWriter(new CSVWriter(new OutputStreamWriter(outputMessage.getBody(), Common.UTF8_CS),separator, quote));
			out.writeAll(records);
			out.close();
		}
	}


}