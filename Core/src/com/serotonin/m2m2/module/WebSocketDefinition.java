/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.web.mvc.websocket.MangoWebSocketHandler;

/**
 * Allow Modules to define WebSockets
 * 
 * @author Terry Packer
 *
 */
public abstract class WebSocketDefinition extends ModuleElementDefinition{

	/**
	 * Return the one and only singleton
	 * @return
	 */
	public abstract MangoWebSocketHandler getHandler();

	/**
	 * The URL to connect to this websocket
	 * 
	 * @return
	 */
	public abstract String getUrl();
	
	/**
	 * Should this handler be wrapper in a Per Connection Handler?
	 * @see org.springframework.web.socket.support.PerConnectionWebSocketHandler
	 * 
	 * @return
	 */
	public abstract boolean perConnection();
	
	
	/**
	 * Return a System Wide Unique Identifier to get this from the registry
	 * @return
	 */
	public abstract String getTypeName();
	
}
