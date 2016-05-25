/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.WebSocketDefinition;
import com.serotonin.m2m2.web.mvc.websocket.MangoWebSocketConfigurer;

/**
 * 
 * Web Socket Configuration
 * 
 * CORS allowed origins in env.properties are set here
 * WebSocketHandlers module definitions are registered here
 * 
 * @author Terry Packer
 *
 */
@Configuration
@EnableWebSocket
public class MangoWebSocketConfiguration extends MangoWebSocketConfigurer {
    
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

		//Setup Allowed Origins for CORS requests
		boolean hasOrigins = false;
		String[] origins = null;
		if(Common.envProps.getBoolean("rest.cors.enabled", false)){
			hasOrigins = true;
			origins = Common.envProps.getStringArray("rest.cors.allowedOrigins", ",", new String[0]);
		}

		List<WebSocketDefinition> defs = ModuleRegistry.getDefinitions(WebSocketDefinition.class);
		for(WebSocketDefinition def : defs){
			WebSocketHandler handler = def.getHandler();
			if(def.perConnection())
				handler = new PerConnectionWebSocketHandler(def.getHandler().getClass());
			WebSocketHandlerRegistration registration = registry.addHandler(handler, def.getUrl())
					.setHandshakeHandler(handshakeHandler())
					.addInterceptors(handshakeIterceptor());
			if(hasOrigins)
				registration.setAllowedOrigins(origins);
		}
		
	}
}
