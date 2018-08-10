/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.websocket;

import javax.servlet.ServletContext;

import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.serotonin.m2m2.Common;

/**
 *
 * @author Terry Packer
 */
@Component
public class MangoWebSocketHandshakeHandler extends DefaultHandshakeHandler {

    //TODO Autowire properties
    public static final int DEFAULT_INPUT_BUFFER_SIZE = 8192;
    public static final long DEFAULT_IDLE_TIMEOUT_MS = 60000L;
    
    public MangoWebSocketHandshakeHandler(@Autowired ServletContext servletContext) {
        super(new JettyRequestUpgradeStrategy(new WebSocketServerFactory(servletContext, getPolicy())));
    }
    
    static WebSocketPolicy getPolicy() {
        int inputBufferSize = Common.envProps.getInt("web.websocket.inputBufferSize", DEFAULT_INPUT_BUFFER_SIZE);
        long idleTimeout = Common.envProps.getLong("web.websocket.idleTimeoutMs", DEFAULT_IDLE_TIMEOUT_MS);

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        policy.setInputBufferSize(inputBufferSize);
        // ping pong mechanism will keep socket alive, web.websocket.pingTimeoutMs should be set lower than the idle timeout
        policy.setIdleTimeout(idleTimeout);
        return policy;
    }
}

