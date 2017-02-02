/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.serotonin.m2m2.module.ModelDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;

/**
 * 
 * @author Terry Packer
 */
public class AbstractModelDeserializer<T> extends StdDeserializer<T> {

	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 * @param vc
	 */
	protected AbstractModelDeserializer(Class<?> vc) {
		super(vc);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		// TODO Auto-generated method stub
		return null;
	}

	
	/**
	 * Find the specific Model definition
	 * 
	 * @return
	 */
	public ModelDefinition findModelDefinition(String typeName) throws ModelNotFoundException{
		List<ModelDefinition> definitions = ModuleRegistry.getModelDefinitions();
		for(ModelDefinition definition : definitions){
			if(definition.getModelTypeName().equalsIgnoreCase(typeName))
				return definition;
		}
		throw new ModelNotFoundException(typeName);
	}
}
