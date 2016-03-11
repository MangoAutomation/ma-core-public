/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.serotonin.m2m2.web.mvc.websocket.MangoWebSocketConfigurer;
import com.serotonin.m2m2.web.mvc.websocket.MangoWebSocketHandshakeInterceptor;
import com.serotonin.m2m2.web.mvc.websocket.publisher.JsonDataWebSocketHandler;

/**
 * @author Terry Packer
 *
 */
@Configuration
@EnableWebSocket
public class MangoWebSocketConfiguration extends MangoWebSocketConfigurer {
    
	public static final JsonDataWebSocketHandler jsonDataHandler = new JsonDataWebSocketHandler();
    
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		
		//TODO add CORS allowed origins in env.properties and set here
		//TODO collect WebSocketHandlers module defintions and register here
		
		registry.addHandler(jsonDataHandler, "/v1/websocket/json-data")
		.setHandshakeHandler(handshakeHandler())
		.addInterceptors(new MangoWebSocketHandshakeInterceptor());
		
	}

}
