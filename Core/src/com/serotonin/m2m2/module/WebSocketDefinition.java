/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.springframework.web.socket.WebSocketHandler;

/**
 * Allow Modules to define WebSockets
 * 
 * @author Terry Packer
 */
public abstract class WebSocketDefinition extends ModuleElementDefinition {

	/* Singleton Instance */
	private WebSocketHandler handler;

	/**
	 * Create the one and only singleton
	 * @return
	 */
	protected abstract WebSocketHandler createHandler();
	
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
	
	
	/**
	 * To be used to get the instance that is loaded into Spring and mapped to this Url
	 * @return
	 */
	public WebSocketHandler getHandlerInstance() {
		if (this.handler == null) {
			this.handler = createHandler();
		}
		return this.handler;
	}
}
