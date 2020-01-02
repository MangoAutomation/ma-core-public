/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Service for access to event handlers
 * 
 * @author Terry Packer
 *
 */
@Service
public class EventHandlerService<T extends AbstractEventHandlerVO<T>> extends AbstractVOService<T, EventHandlerDao<T>> {

    private final EventHandlerCreatePermission createPermission;
    
    @Autowired
    public EventHandlerService(EventHandlerDao<T> dao, PermissionService permissionService, EventHandlerCreatePermission createPermission) {
        super(dao, permissionService);
        this.createPermission = createPermission;
    }

    @Override
    public Set<Role> getCreatePermissionRoles() {
        return createPermission.getRoles();
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
    @EventListener
    protected void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {
        List<T> all = dao.getAll();
        all.stream().forEach((eh) -> {
            eh.getDefinition().handleRoleDeletedEvent(eh, event);
        });
    }
    
    @Override
    public ProcessResult validate(T vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        vo.getDefinition().validate(result, vo, user);
        return result;
    }
    
    @Override
    public ProcessResult validate(T existing, T vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        vo.getDefinition().validate(result, existing, vo, user);
        return result;
    }
    
    private ProcessResult commonValidation(T vo, PermissionHolder user) {
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
        return result;
    }
}
