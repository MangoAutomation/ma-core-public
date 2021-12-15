package com.serotonin.json.spi;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;

/**
 * A more specific version of a class converter. Use this to avoid having to explicitly cast objects to the desired type
 * before performing conversions. The name "serializer" is perhaps misleading, since deserialization is also performed.
 * 
 * This interface can also be used as a substitute for the JsonSerializable interface when the class in question cannot
 * be modified.
 * 
 * @author Matthew Lohbihler
 * 
 * @param <T>
 *            the type that the class serializes.
 */
public interface ClassSerializer<T> {
    /**
     * Write the given object into the JSON writer.
     * 
     * @param writer
     *            the JSON writer to which to write.
     * @param value
     *            the object to write
     */
    void jsonWrite(ObjectWriter writer, T value) throws IOException, JsonException;

    /**
     * Create an object of the type T, and populate it's attributes from the given jsonValue. Attributes that themselves
     * are objects can be populated by forwarding the attribute type back to the JsonReader. Typically, implementations
     * of this method will create the required object and then forward to the jsonRead method below.
     * 
     * @param reader
     *            the current JsonReader
     * @param jsonObject
     *            the JSON value from which attributes can be read
     * @return the created and populated object
     */
    T jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException;

    /**
     * Populate the given object's attributes with values from the given jsonValue.
     * 
     * @param reader
     *            the current JsonReader
     * @param jsonObject
     *            the JSON value from which attributes can be read
     * @param o
     *            the object to populate
     * @return the populated object. Implementation can optionally create a new object if necessary (if, say, the class
     *         in question is immutable)
     */
    T jsonRead(JsonReader reader, JsonObject jsonObject, T o) throws JsonException;
}
