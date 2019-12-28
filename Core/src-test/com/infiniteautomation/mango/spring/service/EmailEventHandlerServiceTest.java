/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * @author Terry Packer
 *
 */
public class EmailEventHandlerServiceTest extends AbstractVOServiceTest<EmailEventHandlerVO, EventHandlerDao<EmailEventHandlerVO>, EventHandlerService<EmailEventHandlerVO>> {
    
    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setRuntimeManager(new MockRuntimeManager(true));
        MockMangoProperties properties = new MockMangoProperties();
        properties.setDefaultValue("security.hashAlgorithm", User.BCRYPT_ALGORITHM);
        lifecycle.setProperties(properties);
        return lifecycle;
    }
    
    @Test(expected = PermissionException.class)
    public void testCreatePrivilegeFails() {
        EmailEventHandlerVO vo = newVO();
        service.insertFull(vo, editUser);
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            EmailEventHandlerVO vo = newVO();
            addRoleToCreatePermission(editRole);
            service.insertFull(vo, editUser);
        });
    }
    
    @SuppressWarnings("unchecked")
    @Override
    EventHandlerService<EmailEventHandlerVO> getService() {
        return Common.getBean(EventHandlerService.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
    EventHandlerDao getDao() {
        return EventHandlerDao.getInstance();
    }

    @Override
    void assertVoEqual(EmailEventHandlerVO expected, EmailEventHandlerVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
        assertRoles(expected.getScriptRoles().getRoles(), actual.getScriptRoles().getRoles());

        //TODO assert remaining
    }

    @Override
    EmailEventHandlerVO newVO() {
        EmailEventHandlerVO vo = (EmailEventHandlerVO) ModuleRegistry.getEventHandlerDefinition(EmailEventHandlerDefinition.TYPE_NAME).baseCreateEventHandlerVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        ScriptPermissions permissions = new ScriptPermissions(Collections.singleton(readRole));
        vo.setScriptRoles(permissions);
        return vo;
    }

    @Override
    EmailEventHandlerVO updateVO(EmailEventHandlerVO existing) {
        existing.setName("new name");
        return existing;
    }
    
    void addRoleToCreatePermission(RoleVO vo) {
        roleService.addRoleToPermission(vo, EventHandlerCreatePermission.PERMISSION, systemSuperadmin);
    }

}
