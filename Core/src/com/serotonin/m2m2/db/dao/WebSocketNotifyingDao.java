/**
 * Copyright (C) 2015 Infinite Automation Systems. All rights reserved.
 * http://infiniteautomation.com/
 */
package com.serotonin.m2m2.db.dao;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler;

/**
 * @author Jared Wiltshire
 */
public abstract class WebSocketNotifyingDao<T extends AbstractVO<T>> extends AbstractDao<T> {
    DaoNotificationWebSocketHandler<T> handler;
    
    public WebSocketNotifyingDao(DaoNotificationWebSocketHandler<T> handler, String typeName, String tablePrefix, String[] extraProperties, String extraSQL) {
        super(typeName, tablePrefix, extraProperties, extraSQL);
        this.handler = handler;
    }

    public WebSocketNotifyingDao(DaoNotificationWebSocketHandler<T> handler, String typeName) {
        super(typeName);
        this.handler = handler;
    }
    
    public void delete(int id, String initiatorId) {
        T vo = get(id);
        delete(vo, initiatorId);
    }
    
    @Override
    public void delete(T vo) {
        delete(vo, null);
    }
    
    public void delete(T vo, String initiatorId) {
        super.delete(vo);
        handler.notify("delete", vo, initiatorId);
    }
    
    public void save(T vo, String initiatorId) {
        if (vo.getId() == Common.NEW_ID) {
            insert(vo, initiatorId);
        }
        else {
            update(vo, initiatorId);
        }
    }

    @Override
    protected void insert(T vo) {
        insert(vo, null);
    }
    
    protected void insert(T vo, String initiatorId) {
        super.insert(vo);
        handler.notify("add", vo, initiatorId);
    }

    @Override
    protected void update(T vo) {
        update(vo, null);
    }
    
    protected void update(T vo, String initiatorId) {
        super.update(vo);
        handler.notify("update", vo, initiatorId);
    }
}
