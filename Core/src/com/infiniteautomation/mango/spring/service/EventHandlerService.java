/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventTypeMatcher;
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
public class EventHandlerService extends AbstractVOService<AbstractEventHandlerVO, EventHandlerDao> {

    private final EventHandlerCreatePermission createPermission;

    private final LoadingCache<EventHandlerKey, List<AbstractEventHandlerVO>> cache = Caffeine.newBuilder()
            .build(k -> dao.enabledHandlersForType(k.eventType, k.eventSubtype));

    @Autowired
    public EventHandlerService(EventHandlerDao dao,
                               PermissionService permissionService,
                               @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EventHandlerCreatePermission createPermission) {
        super(dao, permissionService);
        this.createPermission = createPermission;
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
            List<AbstractEventHandlerVO> all = dao.getAll();
            all.stream().forEach((eh) -> {
                eh.getDefinition().handleRoleEvent(eh, event);
            });
        }
    }

    @Override
    public ProcessResult validate(AbstractEventHandlerVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        vo.getDefinition().validate(result, vo, user);
        permissionService.validatePermission(result, "readPermission", user, vo.getReadPermission());
        permissionService.validatePermission(result, "editPermission", user, vo.getEditPermission());

        return result;
    }

    @Override
    public ProcessResult validate(AbstractEventHandlerVO existing, AbstractEventHandlerVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        vo.getDefinition().validate(result, existing, vo, user);
        permissionService.validatePermission(result, "readPermission", user, vo.getReadPermission());
        permissionService.validatePermission(result, "editPermission", user, vo.getEditPermission());
        return result;
    }

    private ProcessResult commonValidation(AbstractEventHandlerVO vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        //TODO is this true?
        //eventTypes are not validated because it assumed they
        // must be valid to be created and make it into this list

        //Ensure that no 2 are the same
        Set<EventTypeMatcher> types = new HashSet<>(vo.getEventTypes());
        if (vo.getEventTypes().size() != types.size()) {
            //Now find the ones missing from types
            for (EventTypeMatcher type : vo.getEventTypes()) {
                if (!types.contains(type)) {
                    result.addContextualMessage("eventTypes", "eventHandlers.validate.duplicateEventTypes", type.getEventType());
                }
            }
        }
        return result;
    }

    public List<AbstractEventHandlerVO> enabledHandlersForType(EventType type) {
        EventHandlerKey key = new EventHandlerKey(type.getEventType(), type.getEventSubtype());
        List<AbstractEventHandlerVO> results = cache.get(key);

        PermissionHolder user = Common.getUser();

        return Collections.unmodifiableList(results.stream().filter(eh -> {
            boolean hasPermission = permissionService.hasPermission(user, eh.getReadPermission());
            List<EventTypeMatcher> types = eh.getEventTypes();
            return hasPermission && types.stream().anyMatch(t -> t.matches(type));
        }).collect(Collectors.toList()));
    }

    private static final class EventHandlerKey {
        private final String eventType;
        private final String eventSubtype;

        public EventHandlerKey(String eventType, String eventSubtype) {
            this.eventType = eventType;
            this.eventSubtype = eventSubtype;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventHandlerKey that = (EventHandlerKey) o;
            return Objects.equals(eventType, that.eventType) && Objects.equals(eventSubtype, that.eventSubtype);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventType, eventSubtype);
        }
    }
}
