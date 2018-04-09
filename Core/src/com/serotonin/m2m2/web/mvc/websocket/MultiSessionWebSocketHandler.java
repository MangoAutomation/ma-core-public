/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Jared Wiltshire
 */
public abstract class MultiSessionWebSocketHandler extends MangoWebSocketPublisher {
    
    protected final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public MultiSessionWebSocketHandler() {
        super();
    }

    public MultiSessionWebSocketHandler(boolean closeOnLogout) {
        super(closeOnLogout);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        sessions.add(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessions.remove(session);
    }
    
    @Override
    public void httpSessionDestroyed(SessionDestroyedEvent event) {
        String destroyedHttpSessionId = event.getId();
        
        if (this.closeOnLogout) {
            Iterator<WebSocketSession> it = sessions.iterator();
            while (it.hasNext()) {
                WebSocketSession wss = it.next();
                
                String httpSessionId = httpSessionIdForSession(wss);
                if (destroyedHttpSessionId.equals(httpSessionId)) {
                    try {
                        closeSession(wss, NOT_AUTHENTICATED);
                        it.remove();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}
