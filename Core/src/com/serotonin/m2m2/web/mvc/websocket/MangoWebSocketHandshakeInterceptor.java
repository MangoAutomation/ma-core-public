/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.security.Principal;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
@Component
public class MangoWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String HTTP_SESSION_ID_ATTR = "MA_HTTP_SESSION_ID";
    public static final String USER_ATTR = "MA_USER";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        HttpSession session = getSession(request);
        if (session != null) {
            attributes.put(HTTP_SESSION_ID_ATTR, session.getId());
        }

        // get the user at the time of HTTP -> websocket upgrade
        Principal principal = request.getPrincipal();
        if (principal instanceof Authentication) {
            Authentication authentication = (Authentication) principal;

            Object authenticationPrincipal = authentication.getPrincipal();
            if (authenticationPrincipal instanceof User) {
                User user = (User) authenticationPrincipal;
                attributes.put(USER_ATTR, user);
            }
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {

    }

    private HttpSession getSession(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) request;
            return serverRequest.getServletRequest().getSession(false);
        }
        return null;
    }

}
