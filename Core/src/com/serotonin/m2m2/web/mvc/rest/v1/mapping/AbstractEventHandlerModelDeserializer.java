/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers.AbstractEventHandlerModel;

/**
 * 
 * @author Terry Packer
 */
public class AbstractEventHandlerModelDeserializer extends StdDeserializer<AbstractEventHandlerModel<?>>{

	private static final long serialVersionUID = 1L;
	
	protected AbstractEventHandlerModelDeserializer(){
		super(AbstractEventHandlerModel.class);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public AbstractEventHandlerModel<?> deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jp.getCodec();  
		JsonNode tree = jp.readValueAsTree();
		String typeName = tree.get("handlerType").asText();
		EventHandlerDefinition def = ModuleRegistry.getEventHandlerDefinition(typeName);
		if(def == null)
			throw new ModelNotFoundException(typeName);

	    AbstractEventHandlerModel<?> model = (AbstractEventHandlerModel<?>) mapper.treeToValue(tree, def.getModelClass());
	    model.setDefinition(def);
	    return model;
	}
}
