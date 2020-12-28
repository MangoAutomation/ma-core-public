/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.infiniteautomation.mango.spring.db.EventHandlerTableDefinition;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * Service for access to event handlers
 *
 * @author Terry Packer
 *
 */
@Service
public class EventHandlerService extends AbstractVOService<AbstractEventHandlerVO, EventHandlerTableDefinition, EventHandlerDao> implements CachingService {

    private final EventHandlerCreatePermission createPermission;
    private final LoadingCache<EventType, List<AbstractEventHandlerVO>> eventHandlerCache;

    @Autowired
    public EventHandlerService(EventHandlerDao dao, PermissionService permissionService,
                               EventHandlerCreatePermission createPermission,
                               Environment env) {
        super(dao, permissionService);
        this.createPermission = createPermission;

        this.eventHandlerCache = Caffeine.newBuilder()
                .maximumSize(env.getProperty("cache.eventHandlers.size", Long.class, 1000L))
                .build(dao::getEventHandlers);
    }

    @Override
    protected PermissionDefinition getCreatePermission() {
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

    @EventListener
    protected void handleRoleEvent(DaoEvent<? extends RoleVO> event) {
        if (event.getType() == DaoEventType.DELETE) {
            this.eventHandlerCache.invalidateAll();

            dao.getAll((eh, index) -> {
                // event handler definitions might re-serialize their blobs which contain roles
                eh.getDefinition().handleRoleEvent(eh, event);
            });
        }
    }

    @Override
    public ProcessResult validate(AbstractEventHandlerVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        vo.getDefinition().validate(result, vo, user);
        permissionService.validatePermission(result, "readPermission", user, null, vo.getReadPermission());
        permissionService.validatePermission(result, "editPermission", user, null, vo.getEditPermission());

        return result;
    }

    @Override
    public ProcessResult validate(AbstractEventHandlerVO existing, AbstractEventHandlerVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        vo.getDefinition().validate(result, existing, vo, user);
        permissionService.validatePermission(result, "readPermission", user, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validatePermission(result, "editPermission", user, existing.getEditPermission(), vo.getEditPermission());
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

    public List<AbstractEventHandlerVO> getEventHandlers(EventType type) {
        List<AbstractEventHandlerVO> handlers = this.eventHandlerCache.get(type);
        if (handlers == null) {
            return Collections.emptyList();
        }

        PermissionHolder user = Common.getUser();
        return handlers.stream().filter(h -> this.hasReadPermission(user, h)).collect(Collectors.toList());
    }

    @EventListener
    protected void eventHandlerEvent(DaoEvent<? extends AbstractEventHandlerVO> event) {
        if (event.getType() == DaoEventType.DELETE || event.getType() == DaoEventType.UPDATE) {
            this.eventHandlerCache.invalidateAll();
        }
    }

    @Override
    public void clearCaches() {
        this.eventHandlerCache.invalidateAll();
    }
}
