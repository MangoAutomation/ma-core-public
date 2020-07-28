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

import com.infiniteautomation.mango.spring.db.EventHandlerTableDefinition;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
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
public class EventHandlerService extends AbstractVOService<AbstractEventHandlerVO, EventHandlerTableDefinition, EventHandlerDao> {

    private final EventHandlerCreatePermission createPermission;

    @Autowired
    public EventHandlerService(EventHandlerDao dao, PermissionService permissionService, EventHandlerCreatePermission createPermission) {
        super(dao, permissionService);
        this.createPermission = createPermission;
    }

    @Override
    public PermissionDefinition getCreatePermission() {
        return createPermission;
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, AbstractEventHandlerVO vo) {
        return permissionService.hasPermission(user, vo.getEditPermission());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, AbstractEventHandlerVO vo) {
        return permissionService.hasPermission(user, vo.getReadPermission());
    }

    @Override
    @EventListener
    protected void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {
        List<AbstractEventHandlerVO> all = dao.getAll();
        all.stream().forEach((eh) -> {
            eh.getDefinition().handleRoleDeletedEvent(eh, event);
        });
    }

    @Override
    public ProcessResult validate(AbstractEventHandlerVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        vo.getDefinition().validate(result, vo, user);
        permissionService.validateVoRoles(result, "readPermission", user, false, null, vo.getReadPermission());
        permissionService.validateVoRoles(result, "editPermission", user, false, null, vo.getEditPermission());

        return result;
    }

    @Override
    public ProcessResult validate(AbstractEventHandlerVO existing, AbstractEventHandlerVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        vo.getDefinition().validate(result, existing, vo, user);
        permissionService.validateVoRoles(result, "readPermission", user, false, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validateVoRoles(result, "editPermission", user, false, existing.getEditPermission(), vo.getEditPermission());
        return result;
    }

    private ProcessResult commonValidation(AbstractEventHandlerVO vo, PermissionHolder user) {
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
