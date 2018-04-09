/**
 * Copyright (C) 2015 Infinite Automation Systems. All rights reserved.
 * http://infiniteautomation.com/
 */
package com.serotonin.m2m2.web.mvc.websocket;

import org.springframework.web.socket.WebSocketSession;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
public abstract class DaoNotificationWebSocketHandler<T> extends MultiSessionWebSocketHandler {

    /**
     * @param action add, update or delete
     * @param vo
     */
    public void notify(String action, T vo) {
        notify(action, vo, null, null);
    }
    
    /**
     * @param action add, update or delete
     * @param vo
     * @param initiatorId random string to identify who initiated the event
     */
    public void notify(String action, T vo, String initiatorId) {
    	notify(action, vo, initiatorId, null);
    }
    
    /**
     * @param action add, update or delete
     * @param vo
     * @param initiatorId random string to identify who initiated the event
     */
    public void notify(String action, T vo, String initiatorId, String originalXid) {
        for (WebSocketSession session : sessions) {
            User user = getUser(session);
            if (user != null && hasPermission(getUser(session), vo)) {
                notify(session, action, vo, initiatorId, originalXid);
            }
        }
    }
    
    abstract protected boolean hasPermission(User user, T vo);
    abstract protected Object createModel(T vo);
    
    protected void notify(WebSocketSession session, String action, T vo, String initiatorId, String originalXid) {
        try {
            DaoNotificationModel notification = new DaoNotificationModel(action, createModel(vo), initiatorId, originalXid);
            sendMessage(session, notification);
        } catch(WebSocketClosedException e) {
            log.warn("Tried to notify closed websocket session", e);
        } catch (Exception e) {
            try {
                this.sendErrorMessage(session, MangoWebSocketErrorType.SERVER_ERROR, new TranslatableMessage("rest.error.serverError", e.getMessage()));
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
        }
    }
}
