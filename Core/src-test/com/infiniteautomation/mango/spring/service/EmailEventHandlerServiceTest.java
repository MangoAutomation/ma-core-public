/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.infiniteautomation.mango.db.tables.EventHandlers;
import com.infiniteautomation.mango.db.tables.records.EventHandlersRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.rules.ExpectValidationException;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockEventManager;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.type.EventTypeMatcher;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class EmailEventHandlerServiceTest extends AbstractVOServiceTest<AbstractEventHandlerVO, EventHandlersRecord, EventHandlers, EventHandlerDao, EventHandlerService> {

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setRuntimeManager(new MockRuntimeManager(true));
        lifecycle.setEventManager(new MockEventManager(true));
        return lifecycle;
    }

    @Test(expected = PermissionException.class)
    public void testCreatePrivilegeFails() {
        EmailEventHandlerVO vo = newVO(editUser);
        runAs.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        EmailEventHandlerVO vo = newVO(editUser);
        vo.setReadPermission(MangoPermission.requireAnyRole(editRole));
        vo.setEditPermission(MangoPermission.requireAnyRole(editRole));
        ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(editRole));
        vo.setScriptRoles(permissions);
        addRoleToCreatePermission(editRole);
        runAs.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    @Test
    public void testDeleteRoleUpdateVO() {
        EmailEventHandlerVO vo = newVO(readUser);
        ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
        vo.setScriptRoles(permissions);
        service.insert(vo);
        EmailEventHandlerVO fromDb = (EmailEventHandlerVO) service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        roleService.delete(editRole.getId());
        roleService.delete(readRole.getId());
        EmailEventHandlerVO updated = (EmailEventHandlerVO) service.get(fromDb.getId());
        fromDb.setScriptRoles(new ScriptPermissions(Collections.emptySet()));
        assertVoEqual(fromDb, updated);
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        EmailEventHandlerVO vo = newVO(readUser);
        ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
        vo.setScriptRoles(permissions);
        service.update(vo.getXid(), vo);
        EmailEventHandlerVO fromDb = (EmailEventHandlerVO) service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        service.delete(vo.getId());

        service.get(vo.getId());
    }

    @Test
    @ExpectValidationException("scriptRoles")
    public void testCannotInsertUnauthorizedScriptRole() {
        addRoleToCreatePermission(editRole);
        EmailEventHandlerVO vo = newVO(editUser);
        vo.setReadPermission(MangoPermission.requireAnyRole(editRole));
        vo.setEditPermission(MangoPermission.requireAnyRole(editRole));
        ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
        vo.setScriptRoles(permissions);
        runAs.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    @Test
    @ExpectValidationException("scriptRoles")
    public void testCannotUpdateUnauthorizedScriptRole() {
        addRoleToCreatePermission(editRole);
        EmailEventHandlerVO vo = newVO(editUser);
        vo.setReadPermission(MangoPermission.requireAnyRole(editRole));
        vo.setEditPermission(MangoPermission.requireAnyRole(editRole));
        ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(editRole));
        vo.setScriptRoles(permissions);
        runAs.runAs(editUser, () -> {
            EmailEventHandlerVO fromDb = (EmailEventHandlerVO)service.insert(vo);
            ScriptPermissions newPermissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
            fromDb.setScriptRoles(newPermissions);
            service.update(fromDb.getId(), fromDb);
        });
    }

    @Test
    public void testDeleteHandlerForActiveEvent() {
        SystemEventType type = new SystemEventType(SystemEventType.TYPE_SYSTEM_STARTUP);
        List<EventTypeMatcher> eventTypes = Collections.singletonList(new EventTypeMatcher(type));

        addRoleToCreatePermission(editRole);
        EmailEventHandlerVO vo = newVO(editUser);
        vo.setEventTypes(eventTypes);
        service.insert(vo);

        long timestamp = Common.timer.currentTimeMillis();
        Common.eventManager.raiseEvent(type, timestamp, true, AlarmLevels.CRITICAL,
                new TranslatableMessage("common.default", "testing"), null);

        EventInstance activeEvent = ((EventManagerImpl) Common.eventManager).getById(1);
        List<EventHandlerRT<?>> handlers = activeEvent.getHandlers();
        Assert.assertEquals(1, handlers.size());

        service.delete(vo.getId());

        // Cached handlers still remains in active event
        handlers = activeEvent.getHandlers();
        Assert.assertEquals(1, handlers.size());

        // Handlers will be updated during ack or inactive operation
        Common.eventManager.acknowledgeEventById(activeEvent.getId(), timer.currentTimeMillis(), editUser, null);
        handlers = activeEvent.getHandlers();
        Assert.assertEquals(0, handlers.size());
    }

    @Test
    public void testInsertHandlerForActiveEvent() {
        SystemEventType type = new SystemEventType(SystemEventType.TYPE_SYSTEM_STARTUP);
        List<EventTypeMatcher> eventTypes = Collections.singletonList(new EventTypeMatcher(type));

        addRoleToCreatePermission(editRole);
        EmailEventHandlerVO vo = newVO(editUser);
        vo.setEventTypes(eventTypes);
        service.insert(vo);

        long timestamp = Common.timer.currentTimeMillis();
        Common.eventManager.raiseEvent(type, timestamp, true, AlarmLevels.CRITICAL,
                new TranslatableMessage("common.default", "testing"), null);

        EventInstance activeEvent = ((EventManagerImpl) Common.eventManager).getById(1);
        List<EventHandlerRT<?>> handlers = activeEvent.getHandlers();
        Assert.assertEquals(1, handlers.size());

        EmailEventHandlerVO secondVo = newVO(editUser);
        secondVo.setEventTypes(eventTypes);
        service.insert(secondVo);

        // Cached handlers still remains in active event
        handlers = activeEvent.getHandlers();
        Assert.assertEquals(1, handlers.size());

        // Handlers will be updated during ack or inactive operation
        Common.eventManager.acknowledgeEventById(activeEvent.getId(), timer.currentTimeMillis(), editUser, null);
        handlers = activeEvent.getHandlers();
        Assert.assertEquals(2, handlers.size());
    }

    @Override
    EventHandlerService getService() {
        return Common.getBean(EventHandlerService.class);
    }

    @Override
    EventHandlerDao getDao() {
        return EventHandlerDao.getInstance();
    }

    @Override
    void assertVoEqual(AbstractEventHandlerVO expected, AbstractEventHandlerVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
        assertPermission(expected.getReadPermission(), actual.getReadPermission());
        assertPermission(expected.getEditPermission(), actual.getEditPermission());

        assertRoles(((EmailEventHandlerVO)expected).getScriptRoles().getRoles(), ((EmailEventHandlerVO)actual).getScriptRoles().getRoles());

        List<EventTypeMatcher> actualEventTypes = actual.getEventTypes();
        List<EventTypeMatcher> expectedEventTypes = expected.getEventTypes();
        assertEquals(expectedEventTypes.size(), actualEventTypes.size());
        for (int i = 0;  i < expectedEventTypes.size(); i++) {
            EventTypeMatcher actualEventMatcher = actualEventTypes.get(i);
            EventTypeMatcher expectedEventMatcher = expectedEventTypes.get(i);
            assertEquals(expectedEventMatcher.getEventType(), actualEventMatcher.getEventType());
            assertEquals(expectedEventMatcher.getEventSubtype(), actualEventMatcher.getEventSubtype());
            assertEquals(expectedEventMatcher.getReferenceId1(), actualEventMatcher.getReferenceId1());
            assertEquals(expectedEventMatcher.getReferenceId2(), actualEventMatcher.getReferenceId2());
        }

        //TODO assert remaining
    }

    @Override
    EmailEventHandlerVO newVO(User user) {
        EmailEventHandlerVO vo = (EmailEventHandlerVO) ModuleRegistry.getEventHandlerDefinition(EmailEventHandlerDefinition.TYPE_NAME).baseCreateEventHandlerVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        ScriptPermissions permissions = new ScriptPermissions(Collections.singleton(readRole));
        vo.setScriptRoles(permissions);
        List<EventTypeMatcher> eventTypes = Collections.singletonList(new EventTypeMatcher(new MockEventType(readRole)));
        vo.setEventTypes(eventTypes);
        return vo;
    }

    @Override
    AbstractEventHandlerVO updateVO(AbstractEventHandlerVO existing) {
        EmailEventHandlerVO copy = (EmailEventHandlerVO) existing.copy();
        copy.setName("new name");
        return copy;
    }

    void addRoleToCreatePermission(Role vo) {
        PermissionDefinition def = ModuleRegistry.getPermissionDefinition(EventHandlerCreatePermission.PERMISSION);
        Set<Set<Role>> roleSet = def.getPermission().getRoles();
        Set<Set<Role>> newRoles = new HashSet<>();
        newRoles.add(Collections.singleton(vo));
        for (Set<Role> roles : roleSet) {
            newRoles.add(new HashSet<>(roles));
        }
        Common.getBean(SystemPermissionService.class).update(new MangoPermission(newRoles), def);
    }
}
