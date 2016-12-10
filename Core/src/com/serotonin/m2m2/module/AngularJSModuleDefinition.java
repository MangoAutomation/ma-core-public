/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

/**
 * Define an AngularJS Module
 * 
 * @author Terry Packer
 */
public abstract class AngularJSModuleDefinition extends ModuleElementDefinition {

	
	/**
	 * Get the full path and filename of the AngularJS module file, 
	 * relative to the Module's web directory.
	 * 
	 * @return path relative to web directory of module (Contains starting slash)
	 */
	public abstract String getJavaScriptFilename();

}
