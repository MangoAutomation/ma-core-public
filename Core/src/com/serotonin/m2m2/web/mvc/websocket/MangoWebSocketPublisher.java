/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;
import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.session.AbstractSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionDestroyedEvent;
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
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;
import com.serotonin.m2m2.web.mvc.spring.MangoWebSocketConfiguration.MangoSessionDestroyedEvent;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.BearerAuthenticationToken;

/**
 * TODO Mango 3.4 refactor this class into two base classes, one that is a per session handler and another which has
 * a collection of sessions that it manages. The structure right now is ridiculous.
 * 
 * EventWebSocketDefinition creates an EventEventHandler, gets its class then uses a PerConnectionWebSocketHandler to
 * create an EventEventHandler instance for every connection. Each EventEventHandler instance then creates an EventWebSocketPublisher
 * for each user.
 * 
 * @author Terry Packer
 *
 */
public abstract class MangoWebSocketPublisher extends TextWebSocketHandler {

    public static final int DEFAULT_PING_TIMEOUT_MS = 10000;

    /**
     * Close the socket after our HttpSession is invalidated,
     * Eventually this should work via Events instead of checking when we need to send data.
     */
    protected boolean closeOnLogout = true;
    /**
     * Enable Ping/Pong Connection Tracking
     */
    protected boolean usePingPong = true;
    /**
     * Timeout in ms to wait for Pong response before terminating connection
     */
    protected int pingPongTimeoutMs;
    public static final String RECEIVED_PONG = "receivedPong";
    public static final String PING_PONG_TRACKER_ATTRIBUTE = "MangoPingPongTracker";

    // TODO Mango 3.4 remove this from the constructor and use Spring to autowire it
    protected ObjectMapper jacksonMapper;

    protected final Log log = LogFactory.getLog(this.getClass());

    @Autowired
    protected AuthenticationManager authenticationManager;
    @Autowired
    protected ConfigurableApplicationContext context;
    @Autowired
    protected MangoSessionRegistry sessionRegistry;

    public final static CloseStatus NOT_AUTHENTICATED = new CloseStatus(4001, "Not authenticated");
    public final static CloseStatus NOT_AUTHORIZED = new CloseStatus(4003, "Not authorized");

    public MangoWebSocketPublisher() {
        this.jacksonMapper = MangoRestSpringConfiguration.getObjectMapper();
        this.pingPongTimeoutMs = Common.envProps.getInt("web.websocket.pingTimeoutMs", DEFAULT_PING_TIMEOUT_MS);
    }

    /**
     * Supply your own ObjectMapper
     * @param jacksonMapper
     */
    public MangoWebSocketPublisher(ObjectMapper jacksonMapper) {
        this.jacksonMapper = jacksonMapper;
        this.pingPongTimeoutMs = Common.envProps.getInt("web.websocket.pingTimeoutMs", DEFAULT_PING_TIMEOUT_MS);
    }


    /**
     * 
     * @param jacksonMapper
     * @param closeOnLogout - Close the websocket on HttpSesssion Invalidation?
     * @param usePingPong - Use Ping Pong Connection maintenence
     * @param pingPongTimeoutMs - Ms to wait for Pong
     */
    public MangoWebSocketPublisher(ObjectMapper jacksonMapper, boolean closeOnLogout, boolean usePingPong, int pingPongTimeoutMs) {
        this.jacksonMapper = jacksonMapper;
        this.closeOnLogout = closeOnLogout;
        this.usePingPong = usePingPong;
        this.pingPongTimeoutMs = pingPongTimeoutMs;
    }

    @PostConstruct
    private void postConstruct() {
        ApplicationListener<MangoSessionDestroyedEvent> listener = (event) -> {
            this.sessionDestroyed(event.getOriginalEvent());
        };
        context.addApplicationListener(listener);
    }

    /**
     * Called when a session is destroyed. New method, can't use this until Mango 3.4
     * @param event
     */
    public void sessionDestroyed(SessionDestroyedEvent event) {
        //        String httpSessionId = event.getId();
        //        
        //        for (WebSocketSession wss : sessions) {
        //            String httpSession = httpSessionIdForSession(wss);
        //            if (httpSessionId.equals(httpSession)) {
        //                try {
        //                    wss.close();
        //                } catch (Exception e) {
        //                    
        //                }
        //            }
        //        }
    }

    public String httpSessionIdForSession(WebSocketSession session) {
        return (String) session.getAttributes().get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME);
    }

    /**
     * Send an error message
     * @param session
     * @param errorType
     * @param message
     * @throws JsonProcessingException
     * @throws IOException
     */
    protected void sendErrorMessage(WebSocketSession session, MangoWebSocketErrorType errorType, TranslatableMessage message) throws JsonProcessingException, Exception {
        MangoWebSocketErrorModel error = new MangoWebSocketErrorModel(errorType, message.translate(Common.getTranslations()));
        MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.ERROR, error);
        this.sendStringMessageAsync(session, this.jacksonMapper.writeValueAsString(model));
    }


    /**
     * Send a positive response
     * @param session
     * @param payload
     * @throws JsonProcessingException
     * @throws IOException
     */
    protected void sendMessage(WebSocketSession session, Object payload) throws JsonProcessingException, Exception {
        MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.OK, payload);
        this.sendStringMessageAsync(session, this.jacksonMapper.writeValueAsString(model));
    }

    /**
     * WebSocketSession.sendMessage() is blocking and will throw exceptions on concurrent sends, this method uses the aysnc RemoteEndpoint.sendStringByFuture() method instead
     * 
     * @param session
     * @param message
     * @throws IOException
     */
    protected void sendStringMessageAsync(WebSocketSession session, String message) throws Exception {
        // TODO Mango 3.4 add new exception type for closed session and don't try and send error if it was a closed session exception
        if (!session.isOpen()) {
            throw new Exception("Websocket session is closed, can't send message");
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

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        //Ensure we at the very least close the session, this should be overridden in subclasses and ideally the exception logged first
        session.close(new CloseStatus(CloseStatus.SERVER_ERROR.getCode(), exception.getMessage()));
    }

    /**
     * 
     * TODO remove in Mango 3.4
     * 
     * Return the HttpSession assigned to this websocket session when it was created
     * @param session
     * @return
     */
    @Deprecated
    protected AbstractSession getHttpSession(WebSocketSession session) {
        HttpSession httpSession = (HttpSession) session.getAttributes().get(MangoWebSocketHandshakeInterceptor.HTTP_SESSION_ATTRIBUTE);
        if (httpSession instanceof AbstractSession) {
            return (AbstractSession) httpSession;
        }
        return null;
    }

    /**
     * Gets the Mango user for a WebSocketSession. If there is no user and closeOnLogout is true then the WebSocketSession is closed.
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
        // TODO Mango 3.4 don't close sessions here
        if (this.closeOnLogout) {
            this.closeSession(session, NOT_AUTHENTICATED);
        }

        return null;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (this.usePingPong) {
            this.startPingPong(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Object pingPongTracker = session.getAttributes().get(PING_PONG_TRACKER_ATTRIBUTE);
        if (pingPongTracker instanceof MangoPingPongTracker) {
            ((MangoPingPongTracker) pingPongTracker).shutdown();
        }
    }

    /**
     * Start the Ping/Pong Tracker for this session
     */
    public void startPingPong(WebSocketSession session) throws Exception {
        MangoPingPongTracker pingPongTracker = new MangoPingPongTracker(session, this.pingPongTimeoutMs);
        session.getAttributes().put(PING_PONG_TRACKER_ATTRIBUTE, pingPongTracker);
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        //Let the MangoPingPongTracker know we received a pong
        session.getAttributes().put(RECEIVED_PONG, Boolean.TRUE);
    }
}
