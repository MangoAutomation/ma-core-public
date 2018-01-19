/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;

import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.session.AbstractSession;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;

/**
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
	
	protected ObjectMapper jacksonMapper;
	
	public MangoWebSocketPublisher(){
		this.jacksonMapper = MangoRestSpringConfiguration.getObjectMapper();
		this.pingPongTimeoutMs = Common.envProps.getInt("web.websocket.pingTimeoutMs", DEFAULT_PING_TIMEOUT_MS);
	}

	/**
	 * Supply your own ObjectMapper
	 * @param jacksonMapper
	 */
	public MangoWebSocketPublisher(ObjectMapper jacksonMapper){
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
	public MangoWebSocketPublisher(ObjectMapper jacksonMapper, boolean closeOnLogout, boolean usePingPong, int pingPongTimeoutMs){
		this.jacksonMapper = jacksonMapper;
		this.closeOnLogout = closeOnLogout;
		this.usePingPong = usePingPong;
		this.pingPongTimeoutMs = pingPongTimeoutMs;
	}

	
	
	/**
	 * Send an error message
	 * @param session
	 * @param errorType
	 * @param message
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	protected void sendErrorMessage(WebSocketSession session, MangoWebSocketErrorType errorType, TranslatableMessage message) throws JsonProcessingException, Exception{

		if(closeOnLogout){
			//Check our HttpSession to see if we logged out
			AbstractSession httpSession = getHttpSession(session);
			if(httpSession == null || !httpSession.isValid()){
				session.close();
				return;
			}
		}
		
		if(!session.isOpen())
			throw new Exception("Websocket session is closed, can't send message");

		MangoWebSocketErrorModel error = new MangoWebSocketErrorModel(errorType, message.translate(Common.getTranslations()));
		MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.ERROR, error);
		synchronized (session) {
			//Send message asynchronously
			JettyWebSocketSession jws = (JettyWebSocketSession)session;
			jws.getNativeSession().getRemote().sendStringByFuture(new TextMessage(this.jacksonMapper.writeValueAsBytes(model)).getPayload());
		}
	}
	

	/**
	 * Send a positive response
	 * @param session
	 * @param payload
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	protected void sendMessage(WebSocketSession session, Object payload) throws JsonProcessingException, Exception{

		if(closeOnLogout){
			//Check our HttpSession to see if we logged out
			AbstractSession httpSession = getHttpSession(session);
			if(httpSession == null || !httpSession.isValid()){
				session.close();
				return;
			}
		}
		
		if(!session.isOpen())
			throw new Exception("Websocket session is closed, can't send message");
		
		MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.OK, payload);
		synchronized (session) {
			//Send message asynchronously
			JettyWebSocketSession jws = (JettyWebSocketSession)session;
			jws.getNativeSession().getRemote().sendStringByFuture(new TextMessage(this.jacksonMapper.writeValueAsBytes(model)).getPayload());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#handleTransportError(org.springframework.web.socket.WebSocketSession, java.lang.Throwable)
	 */
	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		//Ensure we at the very least close the session, this should be overridden in subclasses and ideally the exception logged first
		session.close(new CloseStatus(CloseStatus.SERVER_ERROR.getCode(), exception.getMessage()));
	}	
	
	/**
	 * Return the HttpSession assigned to this websocket session when it was created
	 * @param session
	 * @return
	 */
	protected AbstractSession getHttpSession(WebSocketSession session) {
	    HttpSession httpSession = (HttpSession) session.getAttributes().get(MangoWebSocketHandshakeInterceptor.HTTP_SESSION_ATTRIBUTE);
	    if (httpSession instanceof AbstractSession) {
	        return (AbstractSession) httpSession;
	    }
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#afterConnectionEstablished(org.springframework.web.socket.WebSocketSession)
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session)
			throws Exception {
		if(this.usePingPong)
			this.startPingPong(session);
	}
    
	/**
	 * Start the Ping/Pong Tracker for this session
	 */
	public void startPingPong(WebSocketSession session) throws Exception{
		
		MangoPingPongTracker tracker = new MangoPingPongTracker((JettyWebSocketSession)session, this.pingPongTimeoutMs);
		try {
			session.getAttributes().put(RECEIVED_PONG, new Boolean(false));
			synchronized(session){
				session.sendMessage(new PingMessage());
			}
		}finally{
			new TimeoutTask(this.pingPongTimeoutMs, tracker);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#handlePongMessage(org.springframework.web.socket.WebSocketSession, org.springframework.web.socket.PongMessage)
	 */
	@Override
	protected void handlePongMessage(WebSocketSession session,
			PongMessage message) throws Exception {
		//Let the session know we received this pong
		session.getAttributes().put(RECEIVED_PONG, new Boolean(true));
	}
}
