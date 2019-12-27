/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Service for access to event handlers
 * 
 * @author Terry Packer
 *
 */
@Service
public class EventHandlerService<T extends AbstractEventHandlerVO<T>> extends AbstractVOService<T, EventHandlerDao<T>> {

    @Autowired
    public EventHandlerService(EventHandlerDao<T> dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        return user.hasAdminRole();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, T vo) {
        return user.hasAdminRole();
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, T vo) {
        return user.hasAdminRole();
    }

    @Override
    public ProcessResult validate(T vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        //TODO is this true? 
        //eventTypes are not validated because it assumed they 
        // must be valid to be created and make it into this list
        
        //Ensure that no 2 are the same
        if(vo.getEventTypes() != null) {
            Set<EventType> types = new HashSet<>(vo.getEventTypes());
            if(vo.getEventTypes().size() != types.size()) {
                //Now find the ones missing from types
                for(EventType type : vo.getEventTypes()) {
                    if(!types.contains(type)) {
                        result.addContextualMessage("eventTypes", "eventHandlers.validate.duplicateEventTypes", type.getEventType());
                    }
                }
            }
        }
        vo.validate(result);
        return result;
    }
    
}
