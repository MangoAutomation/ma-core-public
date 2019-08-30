/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Terry Packer
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property=AbstractDatabaseModel.VERSION_FIELD)
public abstract class AbstractDatabaseModel {

    public static final String VERSION_FIELD = "version";
    
    /**
     * Ensure the model has a version
     * @return
     */
    public abstract String getVersion();
    
}
