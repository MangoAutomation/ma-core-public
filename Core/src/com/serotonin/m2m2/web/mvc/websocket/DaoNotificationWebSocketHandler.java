/**
 * Copyright (C) 2015 Infinite Automation Systems. All rights reserved.
 * http://infiniteautomation.com/
 */
package com.serotonin.m2m2.web.mvc.websocket;

import org.springframework.web.socket.WebSocketSession;

import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
public abstract class DaoNotificationWebSocketHandler<T extends AbstractBasicVO> extends MultiSessionWebSocketHandler {

    /**
     * @param action add, update or delete
     * @param vo
     * @param initiatorId random string to identify who initiated the event
     */
    public void notify(String action, T vo, String initiatorId, String originalXid) {
        for (WebSocketSession session : sessions) {
            User user = getUser(session);
            if (user != null && hasPermission(user, vo) && isSubscribed(session, action, vo, originalXid)) {
                notify(session, action, vo, initiatorId, originalXid);
            }
        }
    }


    abstract protected boolean hasPermission(User user, T vo);
    abstract protected Object createModel(T vo);

    protected boolean isSubscribed(WebSocketSession session, String action, T vo, String originalXid) {
        return true;
    }

    /**
     * You must annotate the overridden method with @EventListener in order for this to work
     * @param event
     */
    abstract protected void handleDaoEvent(DaoEvent<? extends T> event);

    protected void notify(DaoEvent<? extends T> event) {
        DaoEventType type = event.getType();
        String action = null;
        switch(type) {
            case CREATE: action = "create"; break;
            case DELETE: action = "delete"; break;
            case UPDATE: action = "update"; break;
        }
        this.notify(action, event.getVo(), event.getInitiatorId(), event.getOriginalXid());
    }

    protected void notify(WebSocketSession session, String action, T vo, String initiatorId, String originalXid) {
        try {
            this.sendRawMessage(session, this.createNotification(session, action, vo, initiatorId, originalXid));
        } catch(WebSocketSendException e) {
            log.warn("Error notifying websocket", e);
        } catch (Exception e) {
            try {
                this.sendErrorMessage(session, MangoWebSocketErrorType.SERVER_ERROR, new TranslatableMessage("rest.error.serverError", e.getMessage()));
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
        }
    }

    protected Object createNotification(WebSocketSession session, String action, T vo, String initiatorId, String originalXid) {
        DaoNotificationModel payload = new DaoNotificationModel("create".equals(action) ? "add" : action, createModel(vo), initiatorId, originalXid);
        return new MangoWebSocketResponseModel(MangoWebSocketResponseStatus.OK, payload);
    }
}
