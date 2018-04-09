/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.springframework.web.socket.WebSocketHandler;

/**
 * @author Jared Wiltshire
 */
public abstract class PerConnectionWebSocketDefinition extends WebSocketDefinition {

	@Override
	protected WebSocketHandler createHandler() {
        throw new UnsupportedOperationException("Can't create handler instance for a per connection handler");
    }

	@Override
	public WebSocketHandler getHandlerInstance() {
	    throw new UnsupportedOperationException("Can't get handler instance for a per connection handler");
	}

    public abstract Class<? extends WebSocketHandler> getHandlerClass();
}
