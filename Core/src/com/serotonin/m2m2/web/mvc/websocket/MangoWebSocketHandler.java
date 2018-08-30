/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;

/**
 * @author Terry Packer
 */
public abstract class MangoWebSocketHandler extends TextWebSocketHandler {

    public final static CloseStatus NOT_AUTHENTICATED = new CloseStatus(4001, "Not authenticated");
    public final static CloseStatus NOT_AUTHORIZED = new CloseStatus(4003, "Not authorized");

    public static final int DEFAULT_PING_TIMEOUT_MS = 10000;

    /**
     * If true, close the socket after our HttpSession is invalidated or when the authentication token is not valid.
     */
    protected final boolean authenticationRequired;

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
    @Qualifier(MangoRuntimeContextConfiguration.REST_OBJECT_MAPPER_NAME)
    protected ObjectMapper jacksonMapper;

    @Autowired
    protected MangoWebSocketSessionTracker sessionTracker;

    public MangoWebSocketHandler() {
        this(true);
    }

    public MangoWebSocketHandler(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
        this.pingPongTimeoutMs = Common.envProps.getInt("web.websocket.pingTimeoutMs", DEFAULT_PING_TIMEOUT_MS);
        this.usePingPong = this.pingPongTimeoutMs > 0;
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

        try {
            JettyWebSocketSession jettySession = (JettyWebSocketSession) session;
            jettySession.getNativeSession().getRemote().sendStringByFuture(message);
        } catch (Exception e) {
            throw new WebSocketSendException(e);
        }
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

    protected User getUser(WebSocketSession session) {
        if (this.authenticationRequired) {
            return (User) session.getAttributes().get(MangoWebSocketHandshakeInterceptor.USER_ATTR);
        }
        return null;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // only add sessions which should be closed when the session is destroyed
        if (this.authenticationRequired) {
            this.sessionTracker.afterConnectionEstablished(session);
        }

        if (this.usePingPong) {
            this.startPingPong(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (this.authenticationRequired) {
            this.sessionTracker.afterConnectionClosed(session, status);
        }

        if (this.usePingPong) {
            this.stopPingPong(session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
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
