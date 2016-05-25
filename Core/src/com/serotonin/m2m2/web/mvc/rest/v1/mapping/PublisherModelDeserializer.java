/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublisherModel;


/**
 * @author Terry Packer
 *
 */
public class PublisherModelDeserializer extends StdDeserializer<AbstractPublisherModel<?,?>>{

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	protected PublisherModelDeserializer() {
		super(AbstractPublisherModel.class);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public AbstractPublisherModel<?,?> deserialize(JsonParser jp,
			DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jp.getCodec();  
		JsonNode tree = jp.readValueAsTree();
		
		String typeName = tree.get("modelType").asText();
		PublisherDefinition definition = ModuleRegistry.getPublisherDefinition(typeName);
		if(definition == null)
			throw new ModelNotFoundException(typeName);

		AbstractPublisherModel<?,?> model = (AbstractPublisherModel<?,?>) mapper.treeToValue(tree, definition.getPublisherModelClass());
		model.setDefinition(definition); //Set the definition here
	    return model;
	}
}
