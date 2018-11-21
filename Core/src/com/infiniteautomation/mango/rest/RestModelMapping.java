/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest;

/**
 * Mapping to deliver support for converting between classes 
 * when tranporting payloads in/out of the REST api
 * 
 * @author Terry Packer
 *
 */
public interface RestModelMapping <FROM, TO> {
    
    /**
     * Map the Object 
     * @param o
     * @return
     */
    public TO map(Object o);
    
    public Class<TO> toClass();
    public Class<FROM> fromClass();
    
    /**
     * @param o
     * @param model
     * @return
     */
    public default boolean supportsFrom(Object from, Class<?> toClass) {
        if(from == null)
            return false;
        return (from.getClass() == fromClass() && toClass == toClass());
    }
}
