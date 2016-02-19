/**
 * Copyright (C) 2015 Infinite Automation Systems. All rights reserved.
 * http://infiniteautomation.com/
 */
package com.serotonin.m2m2.web.mvc.websocket;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.User;

/**
 * @author Jared Wiltshire
 */
public abstract class DaoNotificationWebSocketHandler<T extends AbstractVO<T>> extends MangoWebSocketHandler {
    private static final Log LOG = LogFactory.getLog(DaoNotificationWebSocketHandler.class);
    
    final Set<WebSocketSession> sessions = new HashSet<WebSocketSession>();
    final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    	super.afterConnectionEstablished(session);
    	lock.writeLock().lock();
    	try{
    		sessions.add(session);
    	}finally{
    		lock.writeLock().unlock();
    	}
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    	lock.writeLock().lock();
    	try{
    		sessions.remove(session);
    	}finally{
    		lock.writeLock().unlock();
    	}
    }
    
    /**
     * @param action add, update or delete
     * @param vo
     */
    public void notify(String action, T vo) {
        notify(action, vo, null);
    }
    
    /**
     * @param action add, update or delete
     * @param vo
     * @param initiatorId random string to identify who initiated the event
     */
    public void notify(String action, T vo, String initiatorId) {
    	lock.readLock().lock();
    	try{
	        for (WebSocketSession session : sessions) {
	            if (hasPermission(getUser(session), vo)) {
	                notify(session, action, vo, initiatorId);
	            }
	        }
    	}finally{
    		lock.readLock().unlock();
    	}
    }
    
    abstract protected boolean hasPermission(User user, T vo);
    abstract protected Object createModel(T vo);
    
    protected void notify(WebSocketSession session, String action, T vo, String initiatorId) {
        try {
            DaoNotificationModel notification = new DaoNotificationModel(action, createModel(vo), initiatorId);
            sendMessage(session, notification);
        } catch (Exception e) {
            try {
                this.sendErrorMessage(session, MangoWebSocketErrorType.SERVER_ERROR, new TranslatableMessage("rest.error.serverError", e.getMessage()));
            } catch (Exception e1) {
                LOG.error(e1.getMessage(), e1);
            }
        }
    }
}
