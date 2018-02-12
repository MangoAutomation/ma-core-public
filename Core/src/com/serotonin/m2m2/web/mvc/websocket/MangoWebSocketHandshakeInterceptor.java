/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

/**
 * @author Terry Packer
 */
public class MangoWebSocketHandshakeInterceptor extends HttpSessionHandshakeInterceptor{

    // TODO Remove these in Mango 3.4
    public static final String USER_ATTRIBUTE = "user";
    public static final String HTTP_SESSION_ATTRIBUTE = "httpsession";
    
    public MangoWebSocketHandshakeInterceptor() {
        super();
        this.setCopyAllAttributes(false);
        this.setCreateSession(false);
    }
    
	@Override
	public boolean beforeHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws Exception {
		
		//Setup Session Tracking
	    User user = Common.getHttpUser();
		if (user != null) {
		    attributes.put(USER_ATTRIBUTE, user);
		}
		
		HttpSession session = ((ServletServerHttpRequest)request).getServletRequest().getSession(false);
		if (session != null) {
	        attributes.put(HTTP_SESSION_ATTRIBUTE, session);
		}

		return super.beforeHandshake(request, response, wsHandler, attributes);
	}

	@Override
	public void afterHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception ex) {
		super.afterHandshake(request, response, wsHandler, ex);
	}
}
