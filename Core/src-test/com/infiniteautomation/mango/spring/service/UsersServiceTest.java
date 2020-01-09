/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.infiniteautomation.mango.spring.db.UserTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.definitions.permissions.UserCreatePermission;
import com.serotonin.m2m2.module.definitions.permissions.UserEditSelfPermission;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class UsersServiceTest extends AbstractVOServiceWithPermissionsTest<User, UserTableDefinition, UserDao, UsersService> {

    /**
     *
     */
    public UsersServiceTest() {
        super(true, 9000);
    }

    @Test
    @Override
    public void testUpdateViaXid() {
        //We don't have an xid
    }

    @Test()
    @Override
    public void testDeleteViaXid() {
        //We don't have an xid
    }

    /**
     * Test that we can read ourself
     */
    @Test
    @Override
    public void testUserReadRole() {
        runTest(() -> {
            User vo = newVO();
            Common.setUser(systemSuperadmin);
            try {
                vo = service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(vo);
            try {
                User fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
            }finally {
                Common.removeUser();
            }
        });
    }

    /**
     * Test edit self permission
     */
    @Override
    @Test
    public void testUserEditRole() {
        runTest(() -> {
            addRoleToEditSelfPermission(readRole);
            User vo = newVO();
            Common.setUser(systemSuperadmin);
            try {
                vo = service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(vo);
            try {
                vo.setName("I edited myself");
                service.update(vo.getUsername(), vo);
                User updated = service.get(vo.getId());
                assertVoEqual(vo, updated);
            }finally {
                Common.removeUser();
            }
        });
    }

    /**
     * Test edit self permission
     */
    @Test
    public void testUserEditRoleDefaultUser() {
        runTest(() -> {
            User vo = newVO();
            Common.setUser(systemSuperadmin);
            try {
                vo = service.insert(vo);
            }finally {
                Common.removeUser();
            }
            Common.setUser(vo);
            try {
                vo.setName("I edited myself");
                service.update(vo.getUsername(), vo);
                User updated = service.get(vo.getId());
                assertVoEqual(vo, updated);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Override
    @Test(expected = PermissionException.class)
    public void testUserEditRoleFails() {
        runTest(() -> {
            addRoleToEditSelfPermission(editRole);
            Common.setUser(readUser);
            try {
                User toUpdate = service.get(readUser.getId());
                toUpdate.setName("I edited myself");
                toUpdate.setPassword("");
                service.update(toUpdate.getUsername(), toUpdate);
                User updated = service.get(readUser.getId());
                assertVoEqual(toUpdate, updated);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test
    @Override
    public void testCannotRemoveEditAccess() {
        //skipped as not possible
    }

    @Test
    @Override
    public void testAddReadRoleUserDoesNotHave() {
        //cannot edit another user as non-admin so skipped
    }

    @Test
    @Override
    public void testReadRolesCannotBeNull() {
        //skipped as no read roles on a user
    }

    @Test
    @Override
    public void testCannotRemoveReadAccess() {
        //skipped as no read roles on a user
    }

    @Test
    @Override
    public void testEditRolesCannotBeNull() {
        //skipped as no edit roles
    }

    @Test(expected = ValidationException.class)
    @Override
    public void testAddEditRoleUserDoesNotHave() {
        User vo = newVO();
        vo.setRoles(Collections.singleton(readRole));
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
            vo = service.get(vo.getId());
        }finally {
            Common.removeUser();
        }
        Common.setUser(vo);
        try {
            vo.setRoles(Collections.singleton(editRole));
            service.update(vo.getUsername(), vo);
        }finally {
            Common.removeUser();
        }
    }

    @Test
    @Override
    public void testDeleteRoleUpdateVO() {
        runTest(() -> {
            User vo = newVO();
            vo.setRoles(Collections.singleton(readRole));
            Common.setUser(systemSuperadmin);
            try {
                service.insert(vo);
                User fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                roleService.delete(readRole.getId());
                fromDb.setRoles(Collections.emptySet());
                //Check database
                User updated = service.get(fromDb.getId());
                assertVoEqual(fromDb, updated);
                //Check cache
                updated = service.get(fromDb.getUsername());
                assertVoEqual(fromDb, updated);
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            Common.setUser(systemSuperadmin);
            try {
                User vo = insertNewVO();
                vo.setRoles(Collections.singleton(readRole));
                service.update(vo.getUsername(), vo);
                User fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getId());

                //Ensure the mappings are gone
                assertEquals(0, service.getDao().getUserRoles(vo).size());

                service.get(vo.getId());
            }finally {
                Common.removeUser();
            }
        });
    }

    @Test(expected = ValidationException.class)
    public void testRemoveRolesFails() {
        User vo = newVO();
        vo.setRoles(Collections.singleton(readRole));
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
            vo = service.get(vo.getId());
        }finally {
            Common.removeUser();
        }
        Common.setUser(vo);
        try {
            vo.setRoles(Collections.emptySet());
            service.update(vo.getUsername(), vo);
        }finally {
            Common.removeUser();
        }
    }

    @Test(expected = ValidationException.class)
    public void testChangeUsername() {
        User vo = newVO();
        Common.setUser(systemSuperadmin);
        try {
            service.insert(vo);
            vo = service.get(vo.getId());
            vo.setUsername(UUID.randomUUID().toString());
            service.insert(vo);
        }finally {
            Common.removeUser();
        }
    }

    void addRoleToEditSelfPermission(Role vo) {
        String permissionType = getCreatePermissionType();
        if(permissionType != null) {
            roleService.addRoleToPermission(vo, UserEditSelfPermission.PERMISSION, systemSuperadmin);
        }
    }

    @Override
    String getCreatePermissionType() {
        return UserCreatePermission.PERMISSION;
    }

    @Override
    void setReadRoles(Set<Role> roles, User vo) {

    }

    @Override
    void setEditRoles(Set<Role> roles, User vo) {

    }

    @Override
    UsersService getService() {
        return Common.getBean(UsersService.class);
    }

    @Override
    UserDao getDao() {
        return UserDao.getInstance();
    }

    @Override
    void assertVoEqual(User expected, User actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getPassword(), actual.getPassword());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getPhone(), actual.getPhone());
        assertEquals(expected.isDisabled(), actual.isDisabled());
        assertEquals(expected.getHomeUrl(), actual.getHomeUrl());
        assertEquals(expected.getLastLogin(), actual.getLastLogin());
        assertEquals(expected.getReceiveAlarmEmails(), actual.getReceiveAlarmEmails());
        assertEquals(expected.isReceiveOwnAuditEvents(), actual.isReceiveOwnAuditEvents());

        assertEquals(expected.getTimezone(), actual.getTimezone());
        assertEquals(expected.isMuted(), actual.isMuted());
        assertEquals(expected.getLocale(), actual.getLocale());
        assertEquals(expected.getTokenVersion(), actual.getTokenVersion());
        assertEquals(expected.getPasswordVersion(), actual.getPasswordVersion());
        assertEquals(expected.isSessionExpirationOverride(), actual.isSessionExpirationOverride());
        assertEquals(expected.getSessionExpirationPeriods(), actual.getSessionExpirationPeriods());
        assertEquals(expected.getSessionExpirationPeriodType(), actual.getSessionExpirationPeriodType());
        assertEquals(expected.getOrganization(), actual.getOrganization());
        assertEquals(expected.getOrganizationalRole(), actual.getOrganizationalRole());
        assertEquals(expected.getCreated().getTime(), actual.getCreated().getTime());
        assertEquals(expected.getEmailVerified(), actual.getEmailVerified());
        assertEquals(expected.getData(), actual.getData());
        assertRoles(expected.getRoles(), actual.getRoles());

    }

    @Override
    User newVO() {
        User user = new User();
        user.setName("usersServiceTest");
        user.setUsername(UUID.randomUUID().toString());
        user.setPassword(Common.encrypt("usersServiceTest"));
        user.setEmail(UUID.randomUUID().toString() + "@example.com");
        user.setPhone("");
        user.setRoles(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(readRole, editRole))));
        user.setDisabled(false);
        return user;
    }

    @Override
    User updateVO(User existing) {
        existing.setName("usersServiceTest2");
        existing.setPassword(Common.encrypt("usersServiceTest2"));
        existing.setEmail(UUID.randomUUID().toString() + "@example.com");
        existing.setPhone("");
        existing.setRoles(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(readRole, editRole, setRole))));
        existing.setDisabled(false);
        return existing;
    }


    @Test
    @Override
    public void testUserCanDelete() {
        //Nothing as you cannot delete another user unless you are superadmin
    }

    @Test
    public void testUsernameUnique() {
        Common.setUser(systemSuperadmin);
        try {
            User user = insertNewVO();
            assertFalse(service.getDao().isUsernameUnique(user.getUsername(), Common.NEW_ID));
            assertTrue(service.getDao().isUsernameUnique(user.getUsername(), user.getId()));
        }finally {
            Common.removeUser();
        }
    }

    @Test
    public void testEmailUnique() {
        Common.setUser(systemSuperadmin);
        try {
            User user = insertNewVO();
            assertFalse(service.getDao().isEmailUnique(user.getEmail(), Common.NEW_ID));
            assertTrue(service.getDao().isEmailUnique(user.getEmail(), user.getId()));
        }finally {
            Common.removeUser();
        }
    }

    @Test
    public void getUserByEmail() {
        Common.setUser(systemSuperadmin);
        try {
            User user = insertNewVO();
            User dbUser = service.getUserByEmail(user.getEmail());
            assertVoEqual(user, dbUser);
        }finally {
            Common.removeUser();
        }
    }

    @Test
    public void getDisabledUsers() {
        Common.setUser(systemSuperadmin);
        try {
            User user = insertNewVO();
            user.setDisabled(true);
            service.update(user.getId(), user);
            List<User> active = service.getDao().getActiveUsers();
            List<User> all = service.getDao().getAll();
            assertEquals(all.size() - 1, active.size());
        }finally {
            Common.removeUser();
        }
    }
}
