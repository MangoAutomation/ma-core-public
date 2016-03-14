/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.serotonin.m2m2.web.mvc.websocket.MangoWebSocketConfigurer;
import com.serotonin.m2m2.web.mvc.websocket.publisher.JsonDataWebSocketHandler;

/**
 * This class is for use in Mango 2.8.x and until then is only used as a reference for the singleton WebSocketHandlers
 * 
 * TODO add CORS allowed origins in env.properties and set here
 * TODO collect WebSocketHandlers module definitions and register here
 * 
 * @author Terry Packer
 *
 */
//@Configuration
//@EnableWebSocket
public class MangoWebSocketConfiguration extends MangoWebSocketConfigurer {
    
	public static final JsonDataWebSocketHandler jsonDataHandler = new JsonDataWebSocketHandler();
    
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		
//		registry.addHandler(jsonDataHandler, "/v1/websocket/json-data")
//		.setHandshakeHandler(handshakeHandler())
//		.addInterceptors(new MangoWebSocketHandshakeInterceptor());
		
	}

}
