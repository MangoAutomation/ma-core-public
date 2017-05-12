/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleElementDefinition;

/**
 * Class to define Read only settings/information that can be provided
 * 
 * @author Terry Packer
 */
public abstract class ReadOnlySettingDefinition<T> extends ModuleElementDefinition{

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
	
	/**
	 * Get the translation of the description of the setting
	 * @return
	 */
	abstract public TranslatableMessage getDescription();
}
