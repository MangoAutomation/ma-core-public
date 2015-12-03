/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;

import org.eclipse.jetty.server.session.AbstractSession;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;

/**
 * @author Terry Packer
 *
 */
public abstract class MangoWebSocketPublisher extends TextWebSocketHandler {

	/**
	 * Close the socket after our HttpSession is invalidated,
	 * Eventually this should work via Events instead of checking when we need to send data.
	 */
	protected boolean closeOnLogout = true;
	
	protected ObjectMapper jacksonMapper;
	
	public MangoWebSocketPublisher(){
		this.jacksonMapper = MangoRestSpringConfiguration.objectMapper;
	}

	/**
	 * Supply your own ObjectMapper
	 * @param jacksonMapper
	 */
	public MangoWebSocketPublisher(ObjectMapper jacksonMapper){
		this.jacksonMapper = jacksonMapper;
	}
	
	/**
	 * 
	 * @param jacksonMapper
	 * @param closeOnLogout - Close the websocket on HttpSesssion Invalidation?
	 */
	public MangoWebSocketPublisher(ObjectMapper jacksonMapper, boolean closeOnLogout){
		this.jacksonMapper = jacksonMapper;
		this.closeOnLogout = closeOnLogout;
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
			if(!httpSession.isValid()){
				session.close();
				return;
			}
		}
		
		if(!session.isOpen())
			throw new Exception("Websocket session is closed, can't send message");

		MangoWebSocketErrorModel error = new MangoWebSocketErrorModel(errorType, message.translate(Common.getTranslations()));
		MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.ERROR, error);
		synchronized (session) {
	        session.sendMessage(new TextMessage(this.jacksonMapper.writeValueAsBytes(model)));
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
			if(!httpSession.isValid()){
				session.close();
				return;
			}
		}
		
		if(!session.isOpen())
			throw new Exception("Websocket session is closed, can't send message");
		
		MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.OK, payload);
		synchronized (session) {
		    session.sendMessage(new TextMessage(this.jacksonMapper.writeValueAsBytes(model)));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#handleTransportError(org.springframework.web.socket.WebSocketSession, java.lang.Throwable)
	 */
	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		//Ensure we at the very least close the session, this should be overridden in subclasses and ideally the exception logged first
		session.close(CloseStatus.SERVER_ERROR);
	}	
	
	/**
	 * Return the HttpSession assigned to this websocket session when it was created
	 * @param session
	 * @return
	 */
	protected AbstractSession getHttpSession(WebSocketSession session){
		return (AbstractSession)session.getAttributes().get("httpsession");
	}
	
	
}
