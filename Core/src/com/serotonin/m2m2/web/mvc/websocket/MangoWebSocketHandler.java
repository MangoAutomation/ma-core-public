/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.BearerAuthenticationToken;

/**
 * @author Terry Packer
 */
public abstract class MangoWebSocketHandler extends TextWebSocketHandler {

    public final static CloseStatus NOT_AUTHENTICATED = new CloseStatus(4001, "Not authenticated");
    public final static CloseStatus NOT_AUTHORIZED = new CloseStatus(4003, "Not authorized");

    public static final int DEFAULT_PING_TIMEOUT_MS = 10000;

    /**
     * Close the socket after our HttpSession is invalidated or when the authentication token is not valid.
     * i.e. true = authentication required
     */
    protected final boolean closeOnLogout;

    /**
     * Enable Ping/Pong Connection Tracking
     */
    protected final boolean usePingPong;
    /**
     * Timeout in ms to wait for Pong response before terminating connection
     */
    protected int pingPongTimeoutMs;
    public static final String RECEIVED_PONG = "receivedPong";
    public static final String PING_PONG_TRACKER_ATTRIBUTE = "MangoPingPongTracker";

    protected final Log log = LogFactory.getLog(this.getClass());

    @Autowired
    protected ObjectMapper jacksonMapper;
    @Autowired
    protected AuthenticationManager authenticationManager;
    @Autowired
    protected MangoSessionRegistry sessionRegistry;

    public MangoWebSocketHandler() {
        this(true);
    }

    public MangoWebSocketHandler(boolean closeOnLogout) {
        this.closeOnLogout = closeOnLogout;
        this.pingPongTimeoutMs = Common.envProps.getInt("web.websocket.pingTimeoutMs", DEFAULT_PING_TIMEOUT_MS);
        this.usePingPong = this.pingPongTimeoutMs > 0;
    }

    public String httpSessionIdForSession(WebSocketSession session) {
        return (String) session.getAttributes().get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME);
    }

    /**
     * Send an error message
     *
     * @param session
     * @param errorType
     * @param message
     * @throws JsonProcessingException
     * @throws IOException
     */
    protected void sendErrorMessage(WebSocketSession session, MangoWebSocketErrorType errorType, TranslatableMessage message) throws JsonProcessingException {
        MangoWebSocketErrorModel error = new MangoWebSocketErrorModel(errorType, message.translate(Common.getTranslations()));
        MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.ERROR, error);
        this.sendStringMessageAsync(session, this.jacksonMapper.writeValueAsString(model));
    }


    /**
     * Send a positive response
     *
     * @param session
     * @param payload
     * @throws JsonProcessingException
     * @throws IOException
     */
    protected void sendMessage(WebSocketSession session, Object payload) throws JsonProcessingException {
        MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.OK, payload);
        this.sendStringMessageAsync(session, this.jacksonMapper.writeValueAsString(model));
    }

    /**
     * Sends a message raw without wrapping it in a MangoWebSocketResponseModel
     * @param session
     * @param message
     * @throws JsonProcessingException
     */
    protected void sendRawMessage(WebSocketSession session, Object message) throws JsonProcessingException {
        this.sendStringMessageAsync(session, this.jacksonMapper.writeValueAsString(message));
    }

    /**
     * Sends a message raw without wrapping it in a MangoWebSocketResponseModel using a Jackson serialization view
     * @param session
     * @param message
     * @param view
     * @throws JsonProcessingException
     */
    protected void sendRawMessageUsingView(WebSocketSession session, Object message, Class<?> view) throws JsonProcessingException {
        ObjectWriter objectWriter = this.jacksonMapper.writerWithView(view);
        this.sendStringMessageAsync(session, objectWriter.writeValueAsString(message));
    }

    /**
     * WebSocketSession.sendMessage() is blocking and will throw exceptions on concurrent sends, this method uses the aysnc RemoteEndpoint.sendStringByFuture() method instead
     *
     * @param session
     * @param message
     * @throws IOException
     */
    protected void sendStringMessageAsync(WebSocketSession session, String message) {
        if (!session.isOpen()) {
            throw new WebSocketClosedException();
        }

        JettyWebSocketSession jettySession = (JettyWebSocketSession) session;
        jettySession.getNativeSession().getRemote().sendStringByFuture(message);
    }

    protected void closeSession(WebSocketSession session, CloseStatus closeStaus) {
        try {
            session.close(closeStaus);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Error closing websocket session", e);
            }
        }
    }

    /**
     * Gets the Mango user for a WebSocketSession.
     *
     * Will return null when:
     * <ul>
     *   <li>There never was a user</li>
     *   <li>Session was invalidated (user logged out, admin disabled them or changed their password)</li>
     *   <li>JWT auth token has expired, been revoked or the private/public keys changed</li>
     * </ul>
     *
     * TODO Mango 3.4 store the user and authentication in the WebSocketSession attributes using the handshake intercepter.
     * Use the sessionDestroyed/user modified/JWT key changed events to replace the user in the attributes or close the session as appropriate.
     * If we have a user modified and JWT key changed event we don't have to re-parse and re-validate the JWT token every time.
     *
     * @param session
     * @return user or null
     */
    protected User getUser(WebSocketSession session) {
        User user = null;
        Authentication authentication = null;

        // get the user at the time of HTTP -> websocket upgrade
        Principal principal = session.getPrincipal();
        if (principal instanceof Authentication) {
            authentication = (Authentication) principal;
            Object authenticationPrincipal = authentication.getPrincipal();
            if (authenticationPrincipal instanceof User) {
                user = (User) authenticationPrincipal;
            }
        }

        // user should never be null as long as the websocket URLs are protected by Spring Security
        if (user != null) {
            String httpSessionId = httpSessionIdForSession(session);
            if (httpSessionId != null) {
                SessionInformation sessionInformation = sessionRegistry.getSessionInformation(httpSessionId);
                if (sessionInformation != null && !sessionInformation.isExpired()) {
                    // we have a valid session
                    // we dont have to check if the user is disabled etc as the session would be invalidated if the user was modified
                    return user;
                }
            }

            // no valid session, check for an authentication token
            if (authentication instanceof PreAuthenticatedAuthenticationToken) {
                PreAuthenticatedAuthenticationToken token = (PreAuthenticatedAuthenticationToken) authentication;
                Object credentials = token.getCredentials();
                if (credentials instanceof String) {
                    String jwtString = (String) credentials;
                    BearerAuthenticationToken bearerToken = new BearerAuthenticationToken(jwtString);

                    /**
                     * Re-authenticate the token as
                     * a) The user might have been disabled
                     * b) The user's tokens might have been revoked
                     * c) The JWT private key might have changed
                     */
                    try {
                        Authentication newAuthentication = this.authenticationManager.authenticate(bearerToken);
                        Object newPrincipal = newAuthentication.getPrincipal();
                        if (newPrincipal instanceof User) {
                            return (User) newPrincipal;
                        }
                    } catch (AuthenticationException e) {
                        // token is no longer valid
                        // do nothing, just return null
                    }
                }
            }
        }

        // no valid session or authentication token, close session if appropriate and return null
        // TODO Mango 3.4 don't close sessions here (need token revoked event before we can remove this)
        if (this.closeOnLogout) {
            this.closeSession(session, NOT_AUTHENTICATED);
        }

        return null;
    }

    protected Set<WebSocketSession> sessionsForHttpSession(WebSocketSession session) {
        @SuppressWarnings("unchecked")
        Set<WebSocketSession> wssSet = (Set<WebSocketSession>) session.getAttributes().get(MangoWebSocketHandshakeInterceptor.WSS_FOR_HTTP_SESSION_ATTR);
        return wssSet;
    }

    protected void addToSessionsForHttpSession(WebSocketSession session) {
        Set<WebSocketSession> wssSet = sessionsForHttpSession(session);
        if (wssSet != null) {
            wssSet.add(session);
        }
    }

    protected void removeFromSessionsForHttpSession(WebSocketSession session) {
        Set<WebSocketSession> wssSet = sessionsForHttpSession(session);
        if (wssSet != null) {
            wssSet.remove(session);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        addToSessionsForHttpSession(session);

        //session.getAttributes().put(MangoWebSocketHandshakeInterceptor.CLOSE_ON_LOGOUT_ATTR, this.closeOnLogout);

        if (this.usePingPong) {
            this.startPingPong(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        removeFromSessionsForHttpSession(session);
        this.stopPingPong(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        removeFromSessionsForHttpSession(session);
        this.stopPingPong(session);
        if (session.isOpen()) {
            closeSession(session, new CloseStatus(CloseStatus.SERVER_ERROR.getCode(), exception.getMessage()));
        }
    }

    /**
     * Start the Ping/Pong Tracker for this session
     */
    private void startPingPong(WebSocketSession session) throws Exception {
        MangoPingPongTracker pingPongTracker = new MangoPingPongTracker(session, this.pingPongTimeoutMs);
        session.getAttributes().put(PING_PONG_TRACKER_ATTRIBUTE, pingPongTracker);
    }

    /**
     * Stop the Ping/Pong Tracker for this session
     */
    private void stopPingPong(WebSocketSession session) throws Exception {
        Object pingPongTracker = session.getAttributes().get(PING_PONG_TRACKER_ATTRIBUTE);
        if (pingPongTracker instanceof MangoPingPongTracker) {
            ((MangoPingPongTracker) pingPongTracker).shutdown();
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        //Let the MangoPingPongTracker know we received a pong
        session.getAttributes().put(RECEIVED_PONG, Boolean.TRUE);
    }
}
