/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

/**
 * @author Terry Packer
 *
 * This is deprecated, use com.serotonin.m2m2.web.mvc.rest.v1.model.RestModelMapping and 
 *   com.infiniteautomation.mango.rest.v2.model.RestModelMapping
 *   
 *   TODO Mango 3.7 (Remove class)
 *
 */
@Deprecated
public abstract class ModelDefinition extends ModuleElementDefinition{
	
    /**
     * A reference to a human readable and translatable brief description of the model. Key references values in
     * i18n.properties files. Descriptions are used in the system settings model section and so should be as brief
     * as possible.
     * 
     * @return the reference key to the model description.
     */
    abstract public String getModelKey();

    /**
     * An internal identifier for this type of model. Must be unique within an MA instance, and is recommended
     * to have the form "&lt;moduleType&gt;.&lt;modelName&gt;" so as to be unique across all modules.
     * 
     * @return the model type name.
     */
    abstract public String getModelTypeName();
    
    
    /**
     * Create a new fully initialized Model
     * 
     * @return Fully initialized model
     */
    abstract public AbstractRestModel<?> createModel();
    
    /**
     * Define what class(s) does this model support being created from.
     * @param clazz
     * @return - true if this model supports this class
     */
    abstract public boolean supportsClass(Class<?> clazz);
    
    /**
     * Return the model class
     * @return
     */
    abstract public Class<? extends AbstractRestModel<?>> getModelClass();

}
