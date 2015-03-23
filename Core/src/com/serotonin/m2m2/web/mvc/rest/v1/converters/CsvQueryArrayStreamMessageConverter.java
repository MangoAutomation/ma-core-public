/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.converters;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import au.com.bytecode.opencsv.CSVWriter;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;
import com.serotonin.m2m2.web.mvc.rest.v1.model.QueryArrayStream;

/**
 * @author Terry Packer
 *
 */
public class CsvQueryArrayStreamMessageConverter extends AbstractHttpMessageConverter<QueryArrayStream>
{
	  public static final MediaType MEDIA_TYPE = new MediaType("text", "csv");
	  private final char separator, quote;

	  public CsvQueryArrayStreamMessageConverter() {
	    this(CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER);
	  }

	  public CsvQueryArrayStreamMessageConverter(char separator, char quote) {
	    super(MEDIA_TYPE);
	    this.separator = separator;
	    this.quote = quote;
	  }

	  @Override
	  protected boolean supports(Class<?> clazz) {
		  return QueryArrayStream.class.isAssignableFrom(clazz);
	  }

	/* (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	protected QueryArrayStream readInternal(Class<? extends QueryArrayStream> clazz,
			HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		return null; //Not implemented
	}

	/* (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void writeInternal(QueryArrayStream stream, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		
			CSVPojoWriter out = new CSVPojoWriter(new CSVWriter(new OutputStreamWriter(outputMessage.getBody(), Common.UTF8_CS),separator,quote));
			stream.streamData(out);
			out.close();
	}


}