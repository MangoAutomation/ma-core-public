package com.serotonin.json.spi;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonValue;

/**
 * An ObjectFactory is registered into a JSON context and bound to a class. It is responsible for creating a new
 * instance of the class.
 * 
 * @author Matthew Lohbihler
 */
public interface ObjectFactory {
    /**
     * Create a new instance of the class to which it is bound in the JSON context registry, using the given JSON value
     * as a hint to the specific sub class to create if necessary.
     * 
     * @param jsonValue
     *            the JSON value to use (if necessary) as a hint to the subclass to create.
     * @return the created instance.
     */
    Object create(JsonValue jsonValue) throws JsonException;
}
