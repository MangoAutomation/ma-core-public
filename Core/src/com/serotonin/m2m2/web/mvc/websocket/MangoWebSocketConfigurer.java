/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * @author Terry Packer
 *
 */
public abstract class MangoWebSocketConfigurer implements WebSocketConfigurer {

	@Bean
    public DefaultHandshakeHandler handshakeHandler() {

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        policy.setInputBufferSize(8192);
        policy.setIdleTimeout(Integer.MAX_VALUE); //We don't want timeouts..
        WebSocketServerFactory factory = new WebSocketServerFactory(policy);
        
        return new DefaultHandshakeHandler(
                new JettyRequestUpgradeStrategy(factory));
    }
	


	/**
	 * Get the interceptor to fill in the session
	 * @return
	 */
	@Bean
	protected HandshakeInterceptor handshakeIterceptor() {
		return new MangoWebSocketHandshakeInterceptor();
	}
}
