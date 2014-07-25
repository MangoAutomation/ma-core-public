/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.serotonin.json.JsonReader;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 *
 */
public class SeroJsonDeSerializer extends JsonDeserializer<JsonSerializable>{
	private static Logger LOG = Logger.getLogger(SeroJsonDeSerializer.class);

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public JsonSerializable deserialize(JsonParser jParser,
			DeserializationContext context) throws IOException,
			JsonProcessingException {
		LOG.info("Deserializing now.");
		return null;
	}
	
	
}
