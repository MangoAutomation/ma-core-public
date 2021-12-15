package com.serotonin.json.spi;

import java.io.IOException;
import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;

/**
 * Converts an unspecified class from and to JSON. Actually classes are assigned when the converter is registered in the
 * JSON context.
 * 
 * @author Matthew Lohbihler
 */
public interface ClassConverter {
    /**
     * Converts the given object into the equivalent JSON type object.
     * 
     * @param value
     *            the object to write
     */
    JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException;

    /**
     * Write the given object into the JSON writer.
     * 
     * @param writer
     *            the JSON writer to which to write.
     * @param value
     *            the object to write
     */
    void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException;

    /**
     * Create an object of the given type, and populate it's attributes from the given jsonValue. Attributes that
     * themselves are objects can be populated by forwarding the attribute type back to the JsonReader. Typically,
     * implementations of this method will create the required object and then forward to the jsonRead method below.
     * 
     * @param reader
     *            the current JsonReader
     * @param jsonValue
     *            the JSON value from which attributes can be read
     * @param type
     *            the generic type of object to return.
     * @return the created and populated object
     */
    Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException;

    /**
     * Populate the given object's attributes with values from the given jsonValue.
     * 
     * @param reader
     *            the current JsonReader
     * @param jsonValue
     *            the JSON value from which attributes can be read
     * @param obj
     *            the object to populate
     * @param type
     *            the generic type of the given object
     */
    void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) throws JsonException;
}
