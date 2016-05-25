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
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublishedPointModel;


/**
 * @author Terry Packer
 *
 */
public class PublishedPointModelDeserializer extends StdDeserializer<AbstractPublishedPointModel<?>>{

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	protected PublishedPointModelDeserializer() {
		super(AbstractPublishedPointModel.class);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public AbstractPublishedPointModel<?> deserialize(JsonParser jp,
			DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jp.getCodec();  
		JsonNode tree = jp.readValueAsTree();

		//Get the model type which is of the form PUB-POINT-{publisher type name}
		String typeName = tree.get("modelType").asText();
		if(typeName != null)
			typeName = typeName.split("PUB-POINT-")[1];
	
		PublisherDefinition definition = ModuleRegistry.getPublisherDefinition(typeName);
		if(definition == null)
			throw new ModelNotFoundException("PUB-POINT-" + typeName);

		return (AbstractPublishedPointModel<?>) mapper.treeToValue(tree, definition.getPublishedPointModelClass());
	}
}
