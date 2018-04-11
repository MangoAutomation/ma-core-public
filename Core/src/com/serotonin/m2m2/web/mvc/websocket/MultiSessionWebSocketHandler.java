/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Jared Wiltshire
 */
public abstract class MultiSessionWebSocketHandler extends MangoWebSocketHandler {

    protected final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public MultiSessionWebSocketHandler() {
        super();
    }

    public MultiSessionWebSocketHandler(boolean authenticationRequired) {
        super(authenticationRequired);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session);
        super.handleTransportError(session, exception);
    }

}
