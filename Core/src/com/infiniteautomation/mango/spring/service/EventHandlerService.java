/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * 
 * TODO Mango 3.6 we need to implement permissions on Event handlers
 * @author Terry Packer
 *
 */
@Service
public class EventHandlerService<T extends AbstractEventHandlerVO<T>> extends AbstractVOService<T, EventHandlerDao<T>> {

    @Autowired
    public EventHandlerService(EventHandlerDao<T> dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        // TODO Auto-generated method stub
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, T vo) {
        // TODO Auto-generated method stub
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, T vo) {
        // TODO Auto-generated method stub
        return user.hasAdminPermission();
    }


}
