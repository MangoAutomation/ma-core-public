/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

/**
 * @author Terry Packer
 *
 */
/**
 * Mapping to deliver support for converting between classes
 * when transporting payloads in/out of the Database
 *
 * @author Terry Packer
 */
public interface DatabaseModelMapping<F, T> {

    public Class<? extends F> fromClass();
    public Class<? extends T> toClass();

    /**
     * Checks if the mapping supports mapping the object to the desired model class.
     *
     * @param from
     * @param toClass
     * @return true if the mapping supports mapping the object to the desired model class
     */
    public default boolean supports(Object from, Class<?> toClass) {
        return this.fromClass().isAssignableFrom(from.getClass()) &&
                toClass.isAssignableFrom(this.toClass());
    }

    /**
     * Performs the mapping of the object to the desired model class. If null is returned other mappings will be tried in order.
     *
     * @param from
     * @param user
     * @param mapper
     * @return The model object or null
     */
    public T map(Object from, DatabaseModelMapper mapper);

}
