/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

/**
 * Allow Modules to define WebSockets
 * 
 * @author Terry Packer
 */
public abstract class WebSocketDefinition extends ModuleElementDefinition {

	/**
	 * Get the bean name to use to find the Spring Bean
	 * @return
	 */
	public abstract String getWebSocketHandlerBeanName();
		
	/**
	 * The URL to connect to this websocket
	 * 
	 * @return
	 */
	public abstract String getUrl();

	/**
	 * Return a System Wide Unique Identifier to get this from the registry
	 * @return
	 */
	public abstract String getTypeName();
	
}
