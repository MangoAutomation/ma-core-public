/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * @author Terry Packer
 *
 */
public abstract class RestJsonDeserializerDefinition extends ModuleElementDefinition{

    /**
     * A reference to a human readable and translatable brief description of the deserializer. Key references values in
     * i18n.properties files. Descriptions are used in the system settings model section and so should be as brief
     * as possible.
     * 
     * @return the reference key to the deserializer description.
     */
    abstract public String getDeserializerKey();

    /**
     * An internal identifier for this type of model. Must be unique within an MA instance, and is recommended
     * to have the form "&lt;moduleName&gt;.&lt;modelName&gt;" so as to be unique across all modules.
     * 
     * @return the model type name.
     */
    abstract public String getDeserializerTypeName();
    
    
    /**
     * The Deserializer Implementation to use
     * @return
     */
    abstract public JsonDeserializer<? extends Object> getDeserializer();
    
    /**
     * Get the type that can be deserialized
     * @return
     */
    abstract public Class<Object> getType();
	
}
