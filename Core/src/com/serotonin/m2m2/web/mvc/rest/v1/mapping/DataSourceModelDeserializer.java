/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModelDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractDataSourceModel;


/**
 * @author Terry Packer
 *
 */
public class DataSourceModelDeserializer extends StdDeserializer<AbstractDataSourceModel<?>>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	

	/**
	 * 
	 */
	protected DataSourceModelDeserializer() {
		super(AbstractDataSourceModel.class);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public AbstractDataSourceModel<?> deserialize(JsonParser jp,
			DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jp.getCodec();  
		JsonNode tree = jp.readValueAsTree();
		
		String typeName = tree.get("modelType").asText();
		DataSourceDefinition definition = ModuleRegistry.getDataSourceDefinition(typeName);
		if(definition == null)
			throw new ModelNotFoundException(typeName);

		AbstractDataSourceModel<?> model = (AbstractDataSourceModel<?>) mapper.treeToValue(tree, definition.getModelClass());
		model.setDefinition(definition); //Set the definition here
	    return model;
	}

	/**
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
