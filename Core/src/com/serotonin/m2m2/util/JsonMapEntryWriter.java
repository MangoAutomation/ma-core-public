/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util;

import java.io.IOException;
import java.util.HashMap;

import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;

/**
 * 
 * Store values from a JsonSerializable Object
 * @author Terry Packer
 *
 */
public class JsonMapEntryWriter extends HashMap<String,Object> implements ObjectWriter{

	private static final long serialVersionUID = 1L;
	
	public JsonMapEntryWriter(){
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.json.ObjectWriter#writeEntry(java.lang.String, java.lang.Object)
	 */
	@Override
	public void writeEntry(String name, Object value) throws IOException,
			JsonException {
		this.put(name, value);
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.json.ObjectWriter#finish()
	 */
	@Override
	public void finish() throws IOException { }

}
