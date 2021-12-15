package com.serotonin.json.convert;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.ObjectWriter;

public class ObjectJsonWriter implements ObjectWriter {
    private final JsonWriter writer;
    private boolean first = true;

    /**
     * Constructs an ObjectWriter and starts the object serialization by writing a '{' to the JsonWriter.
     *
     */
    public ObjectJsonWriter(JsonWriter writer) throws IOException {
        this.writer = writer;

        writer.append('{');
        writer.increaseIndent();
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
    public void writeEntry(String name, Object value) throws IOException, JsonException {
        if (first)
            first = false;
        else
            writer.append(',');
        writer.indent();
        writer.quote(name);
        writer.append(':');
        writer.writeObject(value);
    }

    /**
     * Closes the object by writing a '}' to the JsonWriter. Remember to always call this method for every ObjectWriter
     * instance.
     *
     */
    @Override
    public void finish() throws IOException {
        writer.decreaseIndent();
        writer.indent();
        writer.append('}');
    }

    /**
     * Access the underlying JsonWriter object. This method is for advanced usage only. For most purposes the writeEntry
     * method is sufficient.
     * 
     * @return the underlying JsonWriter object
     */
    public JsonWriter getWriter() {
        return writer;
    }
}
