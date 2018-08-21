/**
 * Copyright (C) 2015 Infinite Automation Systems. All rights reserved.
 * http://infiniteautomation.com/
 */
package com.serotonin.m2m2.web.mvc.websocket;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.web.socket.WebSocketSession;

import com.serotonin.m2m2.db.dao.DaoEvent;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
public abstract class DaoNotificationWebSocketHandler<T extends AbstractBasicVO> extends MultiSessionWebSocketHandler {

    // TODO Mango 3.5 add to constructor and make final
    @Autowired
    protected ApplicationEventMulticaster eventMulticaster;

    protected final DaoEventListener daoEventListener;
    private final ResolvableType eventType;

    protected DaoNotificationWebSocketHandler() {
        super();
        this.daoEventListener = new DaoEventListener();
        this.eventType = ResolvableType.forClassWithGenerics(DaoEvent.class, supportedClass());
    }

    @PostConstruct
    protected void init() {
        this.eventMulticaster.addApplicationListener(this.daoEventListener);
    }

    @PreDestroy
    protected void destroy() {
        this.eventMulticaster.removeApplicationListener(this.daoEventListener);
    }

    protected class DaoEventListener implements GenericApplicationListener {
        @SuppressWarnings("unchecked")
        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            DaoEvent<?> daoEvent = (DaoEvent<?>) event;
            DaoNotificationWebSocketHandler.this.notify(daoEvent.getType().getAction(), (T) daoEvent.getVo(), daoEvent.getInitiatorId(), daoEvent.getOriginalXid());
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        @Override
        public boolean supportsEventType(ResolvableType eventType) {
            return DaoNotificationWebSocketHandler.this.eventType.isAssignableFrom(eventType);
        }

        @Override
        public boolean supportsSourceType(Class<?> sourceType) {
            return true;
        }
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
    abstract protected Class<? extends T> supportedClass();

    protected void notify(WebSocketSession session, String action, T vo, String initiatorId, String originalXid) {
        try {
            DaoNotificationModel notification = new DaoNotificationModel(action, createModel(vo), initiatorId, originalXid);
            sendMessage(session, notification);
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
}
