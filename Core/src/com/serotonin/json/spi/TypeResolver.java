package com.serotonin.json.spi;

import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonValue;

/**
 * A {@link TypeResolver} is registered into a JSON context and bound to a class. It is responsible for resolving the
 * base
 * class (the class that it is bound to) to a concrete subclass.
 * 
 * @author Matthew Lohbihler
 */
public interface TypeResolver {
    /**
     * Return a reference to a concrete class using the given JSON value as a hint.
     * 
     * @param jsonValue
     *            the JSON value to use (if necessary) as a hint to the subclass to create.
     * @return the resolved type.
     */
    Type resolve(JsonValue jsonValue) throws JsonException;
}
