/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;

/**
 * @author Terry Packer
 *
 */
public class DateConverter extends ImmutableClassConverter {



    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) {
        Instant instant = Instant.ofEpochMilli(((Date)value).getTime());
        return new JsonString(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC).toString());
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException {
        Instant instant = Instant.ofEpochMilli(((Date)value).getTime());
        writer.append("\"");
        writer.append(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC).toString());
        writer.append("\"");
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) {
        return new Date(OffsetDateTime.parse(((JsonString)jsonValue).toString()).toInstant().toEpochMilli());
    }
}