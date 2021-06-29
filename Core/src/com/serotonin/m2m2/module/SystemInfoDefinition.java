/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Class to define Read only settings/information that can be provided
 * 
 * @author Terry Packer
 */
public abstract class SystemInfoDefinition<T> extends ModuleElementDefinition{

	/**
	 * Get the key for the read only setting, used to request this information from the REST api
	 * @return String
	 */
	abstract public String getKey();
	
	/**
	 * Get the value for the read only setting.
	 * @return T
	 */
	abstract public T getValue();
	
	/**
	 * Get the description i18n key
	 * @return String
	 */
	abstract public String getDescriptionKey();
	
	/**
	 * Get a translatable description of this information
	 * @return TranslatableMessage
	 */
	public TranslatableMessage getDescriptionMessage() {
	    return new TranslatableMessage(getDescriptionKey());
	}
}
