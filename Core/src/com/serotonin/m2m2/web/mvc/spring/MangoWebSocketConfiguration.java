/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.spring;

import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PerConnectionWebSocketDefinition;
import com.serotonin.m2m2.module.WebSocketDefinition;

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

    public static final int DEFAULT_INPUT_BUFFER_SIZE = 8192;
    public static final long DEFAULT_IDLE_TIMEOUT_MS = 60000L;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Bean
    public HandshakeHandler handshakeHandler() {
        int inputBufferSize = Common.envProps.getInt("web.websocket.inputBufferSize", DEFAULT_INPUT_BUFFER_SIZE);
        long idleTimeout = Common.envProps.getLong("web.websocket.idleTimeoutMs", DEFAULT_IDLE_TIMEOUT_MS);

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        policy.setInputBufferSize(inputBufferSize);
        // ping pong mechanism will keep socket alive, web.websocket.pingTimeoutMs should be set lower than the idle timeout
        policy.setIdleTimeout(idleTimeout);
        WebSocketServerFactory factory = new WebSocketServerFactory(policy);

        return new DefaultHandshakeHandler(
                new JettyRequestUpgradeStrategy(factory));
    }

    @Autowired
    private HandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        //Setup Allowed Origins for CORS requests
        boolean hasOrigins = false;
        String[] origins = null;
        if (Common.envProps.getBoolean("rest.cors.enabled", false)) {
            hasOrigins = true;
            origins = Common.envProps.getStringArray("rest.cors.allowedOrigins", ",", new String[0]);
        }

        for (WebSocketDefinition def : ModuleRegistry.getDefinitions(WebSocketDefinition.class)) {
            WebSocketHandler handler;

            if (def instanceof PerConnectionWebSocketDefinition) {
                PerConnectionWebSocketHandler perConnection = new PerConnectionWebSocketHandler(((PerConnectionWebSocketDefinition) def).getHandlerClass());
                beanFactory.initializeBean(perConnection, PerConnectionWebSocketHandler.class.getName());
                handler = perConnection;
            } else {
                handler = def.getHandlerInstance();
                beanFactory.autowireBean(handler);
                handler = (WebSocketHandler) beanFactory.initializeBean(handler, handler.getClass().getName());
            }

            WebSocketHandlerRegistration registration = registry.addHandler(handler, def.getUrl())
                    .setHandshakeHandler(handshakeHandler())
                    .addInterceptors(handshakeInterceptor);

            if(hasOrigins)
                registration.setAllowedOrigins(origins);
        }
    }
}
