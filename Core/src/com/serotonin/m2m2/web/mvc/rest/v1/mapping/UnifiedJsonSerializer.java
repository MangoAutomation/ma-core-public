/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.serotonin.json.spi.JsonSerializable;

/**
 * @author Terry Packer
 *
 */
public final class UnifiedJsonSerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(Object object, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (object instanceof JsonSerializable) {
            System.out.println("Whoohooo!");
        } else  {
           System.out.println("Ok");
        }
    }
}
