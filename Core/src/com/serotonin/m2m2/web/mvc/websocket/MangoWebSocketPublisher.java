/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.io.IOException;

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


	protected ObjectMapper jacksonMapper;
	
	public MangoWebSocketPublisher(){
		this.jacksonMapper = MangoRestSpringConfiguration.objectMapper;
	}
	
	public MangoWebSocketPublisher(ObjectMapper jacksonMapper){
		this.jacksonMapper = jacksonMapper;
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
		MangoWebSocketResponseModel model = new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.OK, payload);
		synchronized (session) {
		    session.sendMessage(new TextMessage(this.jacksonMapper.writeValueAsBytes(model)));
		}
	}
}
