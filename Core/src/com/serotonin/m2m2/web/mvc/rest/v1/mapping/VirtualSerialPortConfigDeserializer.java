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
import com.infiniteautomation.mango.io.serial.virtual.SerialServerSocketBridgeConfig;
import com.infiniteautomation.mango.io.serial.virtual.SerialSocketBridgeConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;

/**
 * 
 * @author Terry Packer
 */
public class VirtualSerialPortConfigDeserializer extends StdDeserializer<VirtualSerialPortConfig>{

	private static final long serialVersionUID = 1L;
	
	protected VirtualSerialPortConfigDeserializer() {
		super(VirtualSerialPortConfig.class);
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public VirtualSerialPortConfig deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jp.getCodec();  
		JsonNode tree = jp.readValueAsTree();
		String typeName = tree.get("portType").asText();
		Class<?> clazz;
		switch(typeName){
		case "SERIAL_SOCKET_BRIDGE":
			clazz = SerialSocketBridgeConfig.class;
			break;
		case "SERIAL_SERVER_SOCKET_BRIDGE":
			clazz = SerialServerSocketBridgeConfig.class;
			break;
		default:
			throw new ModelNotFoundException(typeName);
		}
		
		VirtualSerialPortConfig model = (VirtualSerialPortConfig)mapper.treeToValue(tree, clazz);
		return model;
	}

}
