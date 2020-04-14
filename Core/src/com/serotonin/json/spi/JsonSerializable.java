package com.serotonin.json.spi;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;

/**
 * This interface can be used to provide custom conversion code within a class itself.
 *
 * @author Matthew Lohbihler
 */
public interface JsonSerializable {
    /**
     * Write the implementing object into the given object writer.
     *
     * @param writer
     *            the object writer to which to write.
     * @throws IOException
     * @throws JsonException
     */
    void jsonWrite(ObjectWriter writer) throws IOException, JsonException;

    /**
     * Read this object from the given JsonObject instance. This object will have been created by a registered
     * ObjectFactory.
     *
     * @param reader
     *            the JSON reader
     * @param jsonObject
     *            the JSON object from which to read attributes for this object
     * @throws JsonException
     */
    void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException;

}
