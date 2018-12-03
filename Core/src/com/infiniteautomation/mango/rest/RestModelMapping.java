/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest;

import com.serotonin.m2m2.vo.User;

/**
 * Mapping to deliver support for converting between classes
 * when transporting payloads in/out of the REST API
 *
 * @author Terry Packer
 */
public interface RestModelMapping<F, T> {

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
     * @return The model object or null
     */
    public T map(Object from, User user);

    /**
     * Returns the view to use when serializing the mapped object
     *
     * @param from
     * @param user
     * @return
     */
    public default Class<?> view(Object from, User user) {
        return null;
    }

}
