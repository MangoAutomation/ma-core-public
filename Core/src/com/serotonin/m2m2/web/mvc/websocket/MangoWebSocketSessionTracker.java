/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;
import java.security.Principal;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.events.AuthTokensRevokedEvent;
import com.serotonin.m2m2.web.mvc.spring.events.UserDeletedEvent;
import com.serotonin.m2m2.web.mvc.spring.events.UserUpdatedEvent;
import com.serotonin.m2m2.web.mvc.spring.events.UserUpdatedEvent.UpdatedFields;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.JwtAuthentication;

/**
 * Tracks websocket sessions by HTTP session, JWT token, user id etc and closes the sessions whenever the authentication is no longer valid.
 *
 * @author Jared Wiltshire
 */
@Service
public class MangoWebSocketSessionTracker {

    public static final String CLOSE_TIMEOUT_TASK_ATTR = "MA_CLOSE_TIMEOUT_TASK";

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Map of http session ids to a set of websocket sessions which are associated with it
     */
    private final SetMultimap<String, WebSocketSession> sessionsByHttpSessionId = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    /**
     * Map of user ids to a set of websocket sessions which are associated with it
     */
    private final SetMultimap<Integer, WebSocketSession> sessionsByUserId = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    /**
     * Set of sessions which were established using JWT authentication
     */
    private final Set<WebSocketSession> jwtSessions = ConcurrentHashMap.newKeySet();

    @Autowired
    private ConfigurableApplicationContext context;

    @PostConstruct
    private void postConstruct() {
        // SessionDestroyedEvent from root context are not propagated to the child web context. Register as a listener
        // on the parent.
        ConfigurableApplicationContext parent = (ConfigurableApplicationContext) context.getParent();

        ApplicationListener<SessionDestroyedEvent> sessionDestroyedListener = this::sessionDestroyed;
        ApplicationListener<UserUpdatedEvent> userUpdatedListener = this::userUpdated;
        ApplicationListener<UserDeletedEvent> userDeletedListener = this::userDeleted;
        ApplicationListener<AuthTokensRevokedEvent> allAuthTokensRevokedListener = this::allAuthTokensRevoked;

        parent.addApplicationListener(sessionDestroyedListener);
        parent.addApplicationListener(userUpdatedListener);
        parent.addApplicationListener(userDeletedListener);
        parent.addApplicationListener(allAuthTokensRevokedListener);
    }

    private String httpSessionIdForSession(WebSocketSession session) {
        return (String) session.getAttributes().get(MangoWebSocketHandshakeInterceptor.HTTP_SESSION_ID_ATTR);
    }

    private User userForSession(WebSocketSession session) {
        return (User) session.getAttributes().get(MangoWebSocketHandshakeInterceptor.USER_ATTR);
    }

    private Authentication authenticationForSession(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal instanceof Authentication) {
            return (Authentication) principal;
        }
        return null;
    }

    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(MangoWebSocketHandler.NOT_AUTHENTICATED);
            }
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Couldn't close WebSocket session " + session.getId(), e);
            }
        }
    }

    private void sessionDestroyed(SessionDestroyedEvent event) {
        String httpSessionId = event.getId();

        for (WebSocketSession session : sessionsByHttpSessionId.removeAll(httpSessionId)) {
            closeSession(session);
        }
    }

    private void userDeleted(UserDeletedEvent event) {
        int userId = event.getUser().getId();

        for (WebSocketSession session : sessionsByUserId.removeAll(userId)) {
            closeSession(session);
        }
    }

    private void userUpdated(UserUpdatedEvent event) {
        User updatedUser = event.getUser();
        int userId = updatedUser.getId();
        EnumSet<UpdatedFields> fields = event.getUpdatedFields();

        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        synchronized (sessionsByUserId) {
            for (WebSocketSession session : sessions) {
                if (updatedUser.isDisabled() || fields.contains(UpdatedFields.PERMISSIONS)) {
                    closeSession(session);
                    continue;
                }

                Authentication authentication = this.authenticationForSession(session);
                if (authentication instanceof JwtAuthentication) {
                    if (fields.contains(UpdatedFields.AUTH_TOKEN)) {
                        closeSession(session);
                        continue;
                    }
                } else if (fields.contains(UpdatedFields.PASSWORD)) {
                    // session auth or basic auth
                    closeSession(session);
                    continue;
                }

                // store the updated user in the session attributes
                session.getAttributes().put(MangoWebSocketHandshakeInterceptor.USER_ATTR, updatedUser);
            }
        }
    }

    private void allAuthTokensRevoked(AuthTokensRevokedEvent event) {
        Iterator<WebSocketSession> it = jwtSessions.iterator();
        while (it.hasNext()) {
            WebSocketSession session = it.next();
            closeSession(session);
            it.remove();
        }
    }

    public void afterConnectionEstablished(WebSocketSession session) {
        String httpSessionId = this.httpSessionIdForSession(session);
        if (httpSessionId != null) {
            sessionsByHttpSessionId.put(httpSessionId, session);
        }

        User user = this.userForSession(session);
        if (user != null) {
            sessionsByUserId.put(user.getId(), session);
        }

        Authentication authentication = this.authenticationForSession(session);
        if (authentication instanceof JwtAuthentication) {
            JwtAuthentication jwtAuthentication = (JwtAuthentication) authentication;
            Date expiration = jwtAuthentication.getToken().getBody().getExpiration();

            TimeoutTask closeTask = new TimeoutTask(expiration, new CloseSessionTask(session));
            session.getAttributes().put(CLOSE_TIMEOUT_TASK_ATTR, closeTask);

            jwtSessions.add(session);
        }
    }

    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String httpSessionId = this.httpSessionIdForSession(session);
        if (httpSessionId != null) {
            sessionsByHttpSessionId.remove(httpSessionId, session);
        }

        User user = this.userForSession(session);
        if (user != null) {
            sessionsByUserId.remove(user.getId(), session);
        }

        jwtSessions.remove(session);

        TimeoutTask closeTask = (TimeoutTask) session.getAttributes().get(CLOSE_TIMEOUT_TASK_ATTR);
        if (closeTask != null) {
            closeTask.cancel();
        }
    }

    public class CloseSessionTask extends TimeoutClient {
        private final WebSocketSession session;

        CloseSessionTask(WebSocketSession session) {
            this.session = session;
        }

        @Override
        public void scheduleTimeout(long fireTime) {
            closeSession(session);
        }

        @Override
        public String getThreadName() {
            return "Close websocket session task";
        }
    }

}
