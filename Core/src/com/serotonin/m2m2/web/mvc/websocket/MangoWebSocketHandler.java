/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.m2m2.vo.User;

/**
 * @author Terry Packer
 *
 */
public abstract class MangoWebSocketHandler extends MangoWebSocketPublisher{

	public MangoWebSocketHandler(){
		super();
	}

	public MangoWebSocketHandler(ObjectMapper jacksonMapper){
		super(jacksonMapper);
	}
	
	/**
	 * @param session
	 * @return
	 */
	protected User getUser(WebSocketSession session) {
		return (User)session.getAttributes().get("user");
	}
	
}
