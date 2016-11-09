/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.module;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Terry Packer
 *
 */
abstract public class JsonRestJacksonModuleDefinition extends ModuleElementDefinition {
	
	/**
	 * Get the Jackson Module to apply to the Main Jackson Mapper
	 */
	public abstract SimpleModule getJacksonModule();

}
