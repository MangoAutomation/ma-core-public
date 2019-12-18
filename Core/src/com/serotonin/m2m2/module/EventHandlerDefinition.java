/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;

/**
 * Provides modules with the ability to register additional event handlers
 * 
 * @author Terry Packer
 *
 */
public abstract class EventHandlerDefinition<T extends AbstractEventHandlerVO<T>> extends ModuleElementDefinition{

    /**
     * An internal identifier for this type of Event Handler. Must be unique within an MA instance, and is recommended
     * to have the form "&lt;moduleType&gt;.&lt;modelName&gt;" so as to be unique across all modules.
     * 
     * @return the model type name.
     */
    abstract public String getEventHandlerTypeName();
	
    /**
     * A reference to a human readable and translatable brief description of the handler. Key reference values in
     * i18n.properties files. Descriptions are used in drop down select boxes, and so should be as brief as possible.
     * 
     * @return the reference key to the handler's short description.
     */
    abstract public String getDescriptionKey();
    
    /**
     * Create and return an instance of the event handler
     * 
     * @return a new instance of the event handler.
     */
    abstract protected T createEventHandlerVO();

    
    /**
     * Used by MA core code to create a new event handler instances as required. Should not be used by client code.
     */
    public final T baseCreateEventHandlerVO() {
        T handler = createEventHandlerVO();
        handler.setDefinition(this);
        return handler;
    }
    
}
