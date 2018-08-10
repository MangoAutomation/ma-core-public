/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.csv;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.script.ScriptPermissions;

/**
 * @author Terry Packer
 *
 */
public class ScriptPermissionsPropertyEditor extends CSVPropertyEditor{

	private static final Log LOG = LogFactory.getLog(ScriptPermissionsPropertyEditor.class);

	private ScriptPermissions permissions;
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) {
		this.permissions = (ScriptPermissions)value;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#getValue()
	 */
	@Override
	public Object getValue() {
		return this.permissions;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#getAsText()
	 */
	@Override
	public String getAsText() {
		try {
	        return Common.getBean(ObjectMapper.class)
	                .writerFor(ScriptPermissions.class)
	                .writeValueAsString(this.permissions);
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage(), e);
			return "";
		} 
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPropertyEditor#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			this.permissions = Common.getBean(ObjectMapper.class).readerFor(ScriptPermissions.class).readValue(text);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

}
