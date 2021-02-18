/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.TranslatableMessageParseException;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;

public class PointValueTimeDeserializer extends StdDeserializer<PointValueTime> {

    protected PointValueTimeDeserializer() {
        super(PointValueTime.class);
    }

    @Override
    public PointValueTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectCodec codec = jsonParser.getCodec();
        JsonNode node = codec.readTree(jsonParser);

        JsonNode dataTypeNode = node.get("dataType");
        if (dataTypeNode == null) {
            throw JsonMappingException.from(jsonParser, "Missing dataType");
        }
        JsonNode valueNode = node.get("value");
        if (valueNode == null) {
            throw JsonMappingException.from(jsonParser, "Missing value");
        }
        JsonNode timestampNode = node.get("timestamp");
        if (timestampNode == null) {
            throw JsonMappingException.from(jsonParser, "Missing timestamp");
        }

        long timestamp = timestampNode.asLong();

        String dataTypeStr = dataTypeNode.asText();
        DataValue dataValue;
        switch (dataTypeStr) {
            case "ALPHANUMERIC": dataValue = new AlphanumericValue(valueNode.asText()); break;
            case "BINARY": dataValue = new BinaryValue(valueNode.asBoolean()); break;
            case "MULTISTATE": dataValue = new MultistateValue(valueNode.asInt()); break;
            case "NUMERIC": dataValue = new NumericValue(valueNode.asDouble()); break;
            case "IMAGE": throw JsonMappingException.from(jsonParser, "Unsupported dataType " + dataTypeStr);
            default: throw JsonMappingException.from(jsonParser, "Unknown dataType " + dataTypeStr);
        }

        JsonNode annotationNode = node.get("serializedAnnotation");
        if (annotationNode != null && !annotationNode.isNull()) {
            try {
                return new AnnotatedPointValueTime(dataValue, timestamp, TranslatableMessage.deserialize(annotationNode.asText()));
            } catch (TranslatableMessageParseException e) {
                throw JsonMappingException.from(jsonParser, "Can't deserialize annotation", e);
            }
        }

        return new PointValueTime(dataValue, timestamp);
    }
}
