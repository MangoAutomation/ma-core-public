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
        getService().permissionService.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            EmailEventHandlerVO vo = newVO(editUser);
            ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
            vo.setScriptRoles(permissions);
            addRoleToCreatePermission(editRole);
            getService().permissionService.runAs(editUser, () -> {
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
            getService().permissionService.runAsSystemAdmin(() -> {
                service.insert(vo);
                EmailEventHandlerVO fromDb = (EmailEventHandlerVO) service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                roleService.delete(editRole.getId());
                roleService.delete(readRole.getId());
                EmailEventHandlerVO updated = (EmailEventHandlerVO) service.get(fromDb.getId());
                fromDb.setScriptRoles(new ScriptPermissions(Collections.emptySet()));
                assertVoEqual(fromDb, updated);
            });
        });
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            EmailEventHandlerVO vo = newVO(readUser);
            ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
            vo.setScriptRoles(permissions);
            getService().permissionService.runAsSystemAdmin(() -> {
                service.update(vo.getXid(), vo);
                EmailEventHandlerVO fromDb = (EmailEventHandlerVO) service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getId());

                //Ensure the mappings are gone
                assertEquals(0, roleService.getDao().getPermission(vo, PermissionService.SCRIPT).getUniqueRoles().size());

                service.get(vo.getId());
            });
        });
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
        assertPermission(((EmailEventHandlerVO)expected).getScriptRoles().getPermission(), ((EmailEventHandlerVO)actual).getScriptRoles().getPermission());

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
        getService().permissionService.runAsSystemAdmin(() -> {
            PermissionDefinition def = ModuleRegistry.getPermissionDefinition(EventHandlerCreatePermission.PERMISSION);
            Set<Set<Role>> roleSet = def.getPermission().getRoles();
            Set<Set<Role>> newRoles = new HashSet<>();
            newRoles.add(Collections.singleton(vo));
            for(Set<Role> roles : roleSet) {
                newRoles.add(new HashSet<>(roles));
            }
            def.update(newRoles);
        });
    }
}
