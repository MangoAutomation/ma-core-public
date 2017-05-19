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
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors.AbstractEventDetectorModel;

/**
 * 
 * @author Terry Packer
 */
public class AbstractEventDetectorModelDeserializer extends StdDeserializer<AbstractEventDetectorModel<?>>{

	private static final long serialVersionUID = 1L;
	
	protected AbstractEventDetectorModelDeserializer(){
		super(AbstractEventDetectorModel.class);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public AbstractEventDetectorModel<?> deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jp.getCodec();  
		JsonNode tree = jp.readValueAsTree();
		String typeName = tree.get("detectorType").asText();
		EventDetectorDefinition<?> def = ModuleRegistry.getEventDetectorDefinition(typeName);
		if(def == null)
			throw new ModelNotFoundException(typeName);

	    AbstractEventDetectorModel model = (AbstractEventDetectorModel<?>) mapper.treeToValue(tree, def.getModelClass());
	    model.setDefinition(def);
	    return model;
	}
}
