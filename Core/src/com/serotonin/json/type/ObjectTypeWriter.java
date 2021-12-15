package com.serotonin.json.type;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;

public class ObjectTypeWriter implements ObjectWriter {
    private final JsonTypeWriter writer;
    private final JsonObject jsonObject = new JsonObject();

    /**
     * Constructs an ObjectWriter and starts the object serialization by writing a '{' to the JsonWriter.
     *
     */
    public ObjectTypeWriter(JsonTypeWriter writer) {
        this.writer = writer;
    }

    /**
     * Writes a single object to the JsonWriter
     * 
     * @param name
     *            the object attribute name
     * @param value
     *            the object to write. This will be serialized like any other object.
     *
     */
    @Override
    public void writeEntry(String name, Object value) throws JsonException {
        jsonObject.put(name, writer.writeObject(value));
    }

    /**
     * Closes the object by writing a '}' to the JsonWriter. Remember to always call this method for every ObjectWriter
     * instance.
     *
     */
    @Override
    public void finish() {
        // no op
    }

    /**
     * @return the jsonObject
     */
    public JsonObject getJsonObject() {
        return jsonObject;
    }
}
