/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

/**
 * Used to ensure Jackson is able to de-serialize the model (type T) in this mapping
 * 
 * @author Terry Packer
 *
 */
public interface DatabaseModelJacksonMapping <F, T> extends DatabaseModelMapping<F, T> {

    /**
     * Return the type name that maps the T class (Model) to a Type in Jackson
     *  in the DAO layer we call this the version
     */
    public String getVersion();
    
}
