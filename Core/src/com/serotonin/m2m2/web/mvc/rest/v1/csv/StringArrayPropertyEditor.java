/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;

/**
 * 
 * Class to serialize arrays for CSV cells
 * @author Terry Packer
 *
 */
public class StringArrayPropertyEditor  extends CSVPropertyEditor{

	private static Log LOG = LogFactory.getLog(StringArrayPropertyEditor.class);
	private String[] data;

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) {
		this.data = (String[]) value;
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#getValue()
	 */
	@Override
	public Object getValue() {
		return data;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#getAsText()
	 */
	@Override
	public String getAsText() {
		StringWriter writer = new StringWriter();
		try {
			MangoRestSpringConfiguration.getObjectMapper().writeValue(writer, data);
			return writer.toString();
		} catch (IOException e) {
			LOG.error(e.getMessage(),e);
			throw new ShouldNeverHappenException(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			this.data = MangoRestSpringConfiguration.getObjectMapper().readValue(text, String[].class);
		} catch (IOException e) {
			LOG.error(e.getMessage(),e);
			throw new ShouldNeverHappenException(e);
		}	
	}
	
	
}
