/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.EventHandlerTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class EmailEventHandlerServiceTest extends AbstractVOServiceTest<AbstractEventHandlerVO, EventHandlerTableDefinition, EventHandlerDao, EventHandlerService> {

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setRuntimeManager(new MockRuntimeManager(true));
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
        runTest(() -> {
            EmailEventHandlerVO vo = newVO(editUser);
            vo.setReadPermission(MangoPermission.requireAnyRole(editRole));
            vo.setEditPermission(MangoPermission.requireAnyRole(editRole));
            ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(editRole));
            vo.setScriptRoles(permissions);
            addRoleToCreatePermission(editRole);
            runAs.runAs(editUser, () -> {
                service.insert(vo);
            });
        });
    }

    @Test
    public void testDeleteRoleUpdateVO() {
        runTest(() -> {
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
        });
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            EmailEventHandlerVO vo = newVO(readUser);
            ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
            vo.setScriptRoles(permissions);
            service.update(vo.getXid(), vo);
            EmailEventHandlerVO fromDb = (EmailEventHandlerVO) service.get(vo.getId());
            assertVoEqual(vo, fromDb);
            service.delete(vo.getId());

            service.get(vo.getId());
        });
    }

    @Test
    public void testCannotInsertUnauthorizedScriptRole() {
        runTest(() -> {
            addRoleToCreatePermission(editRole);
            EmailEventHandlerVO vo = newVO(editUser);
            vo.setReadPermission(MangoPermission.requireAnyRole(editRole));
            vo.setEditPermission(MangoPermission.requireAnyRole(editRole));
            ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
            vo.setScriptRoles(permissions);
            runAs.runAs(editUser, () -> {
                service.insert(vo);
            });
        }, "scriptRoles");
    }

    @Test
    public void testCannotUpdateUnauthorizedScriptRole() {
        runTest(() -> {
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
        }, "scriptRoles");
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

        //TODO assert remaining
    }

    @Override
    EmailEventHandlerVO newVO(User user) {
        EmailEventHandlerVO vo = (EmailEventHandlerVO) ModuleRegistry.getEventHandlerDefinition(EmailEventHandlerDefinition.TYPE_NAME).baseCreateEventHandlerVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        ScriptPermissions permissions = new ScriptPermissions(Collections.singleton(readRole));
        vo.setScriptRoles(permissions);
        return vo;
    }

    @Override
    AbstractEventHandlerVO updateVO(AbstractEventHandlerVO existing) {
        existing.setName("new name");
        return existing;
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
