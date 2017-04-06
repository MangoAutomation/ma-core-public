package com.serotonin.json.test;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.ClassSerializer;
import com.serotonin.json.type.JsonObject;

public class PrimitiveSerializer implements ClassSerializer<Primitives> {
    @Override
    public void jsonWrite(ObjectWriter writer, Primitives primitives) throws IOException, JsonException {
        writer.writeEntry("bigi", primitives.getBigInteger());
        writer.writeEntry("bigd", primitives.getBigDecimal());
    }

    @Override
    public Primitives jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        return jsonRead(reader, jsonObject, new Primitives());
    }

    @Override
    public Primitives jsonRead(JsonReader reader, JsonObject jsonObject, Primitives primitives) throws JsonException {
        primitives.setBigInteger(jsonObject.getBigInteger("bigi"));
        primitives.setBigDecimal(jsonObject.getBigDecimal("bigd"));
        return primitives;
    }
}
