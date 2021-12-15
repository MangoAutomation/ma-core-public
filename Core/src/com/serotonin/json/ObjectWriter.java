package com.serotonin.json;

import java.io.IOException;

/**
 * A convenience class for easily writing properties of an object. Proper usage of this class is important!
 * 
 * When constructed, the ObjectWriter will begin the writing of a JSON object. Every instance must be closed using the
 * finish() method or the resulting JSON will be malformed. An arbitrary number of object properties can be written with
 * the writeEntry() method.
 * 
 * @author Matthew Lohbihler
 */
public interface ObjectWriter {
    /**
     * Writes a single object to the JsonWriter
     * 
     * @param name
     *            the object attribute name
     * @param value
     *            the object to write. This will be serialized like any other object.
     *
     */
    public void writeEntry(String name, Object value) throws IOException, JsonException;

    /**
     * Closes the object by writing a '}' to the JsonWriter. Remember to always call this method for every ObjectWriter
     * instance.
     *
     */
    public void finish() throws IOException;
}
