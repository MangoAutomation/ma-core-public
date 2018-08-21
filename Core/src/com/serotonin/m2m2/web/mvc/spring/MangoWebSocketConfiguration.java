/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

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
@ComponentScan(basePackages = {"com.serotonin.m2m2.web.mvc.websocket"})
public class MangoWebSocketConfiguration implements WebSocketConfigurer {

    @Autowired
    private HandshakeInterceptor handshakeInterceptor;

    @Autowired
    private HandshakeHandler handshakeHandler;

    @Value("${rest.cors.enabled:false}")
    private boolean corsEnabled;

    @Value("${rest.cors.allowedOrigins}")
    private String[] allowedOrigins;

    @Autowired
    private Set<WebSocketHandler> handlers;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        for (WebSocketHandler handler : handlers) {
            WebSocketMapping mapping = handler.getClass().getAnnotation(WebSocketMapping.class);
            if (mapping != null) {
                for (String url : mapping.value()) {
                    WebSocketHandlerRegistration registration = registry.addHandler(handler, url)
                            .setHandshakeHandler(handshakeHandler)
                            .addInterceptors(handshakeInterceptor);

                    // Use allowed origins from CORS configuration
                    if (corsEnabled) {
                        registration.setAllowedOrigins(allowedOrigins);
                    }
                }
            }
        }
    }
}
