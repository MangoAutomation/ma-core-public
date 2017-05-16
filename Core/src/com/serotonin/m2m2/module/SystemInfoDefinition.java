/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

/**
 * Class to define Read only settings/information that can be provided
 * 
 * @author Terry Packer
 */
public abstract class SystemInfoDefinition<T> extends ModuleElementDefinition{

	/**
	 * Get the key for the read only setting, used to request this information from the REST api
	 * @return
	 */
	abstract public String getKey();
	
	/**
	 * Get the value for the read only setting.
	 * @return
	 */
	abstract public T getValue();
}
