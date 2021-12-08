/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.serotonin.m2m2.rt.dataImage.IAnnotated;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

public class PointValueTimeSerializer extends StdSerializer<PointValueTime> {

    protected PointValueTimeSerializer() {
        super(PointValueTime.class);
    }

    @Override
    public void serialize(PointValueTime pointValueTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        DataValue value = pointValueTime.getValue();
        switch(value.getDataType()) {
            case ALPHANUMERIC:
                jsonGenerator.writeStringField("dataType", "ALPHANUMERIC");
                jsonGenerator.writeStringField("value", value.getStringValue());
                break;
            case BINARY:
                jsonGenerator.writeStringField("dataType", "BINARY");
                jsonGenerator.writeBooleanField("value", value.getBooleanValue());
                break;
            case MULTISTATE:
                jsonGenerator.writeStringField("dataType", "MULTISTATE");
                jsonGenerator.writeNumberField("value", value.getIntegerValue());
                break;
            case NUMERIC:
                jsonGenerator.writeStringField("dataType", "NUMERIC");
                jsonGenerator.writeNumberField("value", value.getDoubleValue());
                break;
        }

        jsonGenerator.writeNumberField("timestamp", pointValueTime.getTime());

        if (pointValueTime instanceof IAnnotated) {
            jsonGenerator.writeStringField("serializedAnnotation", ((IAnnotated) pointValueTime).getSourceMessage().serialize());
        }

        jsonGenerator.writeEndObject();
    }
}
