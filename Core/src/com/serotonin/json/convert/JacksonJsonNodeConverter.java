/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.spi.ClassConverter;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;

/**
 * Converter for Jackon JsonNodes
 * @author Terry Packer
 *
 */
public class JacksonJsonNodeConverter implements ClassConverter {

    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        ObjectMapper mapper = Common.getBean(ObjectMapper.class, MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME);
        Object rawValue = mapper.convertValue(value, Object.class);
        return writer.writeObject(rawValue);
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        ObjectMapper mapper = Common.getBean(ObjectMapper.class, MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME);
        Object rawValue = mapper.convertValue(value, Object.class);
        writer.writeObject(rawValue);
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        ObjectMapper mapper = Common.getBean(ObjectMapper.class, MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME);
        Object raw = jsonValue.toNative();
        return mapper.valueToTree(raw);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type)
            throws JsonException {
        throw new ShouldNeverHappenException("Root and Nested Jackson JsonNode types are not supported");
    }

}
