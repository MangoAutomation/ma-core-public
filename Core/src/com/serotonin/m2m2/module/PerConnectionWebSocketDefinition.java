/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.springframework.web.socket.WebSocketHandler;

/**
 * @author Jared Wiltshire
 */
public abstract class PerConnectionWebSocketDefinition extends WebSocketDefinition {

    /**
     * Get the class to instantiate beans of when a new connection is created
     * @return
     */
    public abstract Class<? extends WebSocketHandler> getHandlerClass();
    
    
    @Override
    public String getWebSocketHandlerBeanName() {
        throw new UnsupportedOperationException("Per connection handler's should not be named beans");
    }
}
