package com.serotonin.json.factory;

import com.serotonin.json.JsonException;

/**
 * The default constructor factory. Named because it uses a class's default constructor to create a new instance of an
 * object.
 * 
 * @author Matthew Lohbihler
 */
public class DefaultConstructorFactory {
    /**
     * Create a new instance of the given clazz using the default constructor.
     * 
     * @param clazz
     *            the clazz of the desired object
     * @return the new instance
     * @throws JsonException
     *             if the is an exception while invoking the default constructor.
     */
    public Object create(Class<?> clazz) throws JsonException {
        try {
            return clazz.newInstance();
        }
        catch (InstantiationException e) {
            throw new JsonException(e);
        }
        catch (IllegalAccessException e) {
            throw new JsonException(e);
        }
    }
}
