/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.util;

import java.io.IOException;
import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.spi.ClassConverter;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

/**
 * Convert Rest Models to Sero JSON
 * 
 * @author Terry Packer
 */
public class AbstractRestModelConverter implements ClassConverter{

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.ClassConverter#jsonWrite(com.serotonin.json.type.JsonTypeWriter, java.lang.Object)
	 */
	@Override
	public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
		return writer.writeObject(((AbstractRestModel<?>)value).getData());
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.ClassConverter#jsonWrite(com.serotonin.json.JsonWriter, java.lang.Object)
	 */
	@Override
	public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
		writer.writeObject(((AbstractRestModel<?>)value).getData());
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.ClassConverter#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonValue, java.lang.reflect.Type)
	 */
	@Override
	public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
		throw new JsonException("Unsupported.");
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.ClassConverter#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonValue, java.lang.Object, java.lang.reflect.Type)
	 */
	@Override
	public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) throws JsonException {
		throw new JsonException("Unsupported.");
	}

}
