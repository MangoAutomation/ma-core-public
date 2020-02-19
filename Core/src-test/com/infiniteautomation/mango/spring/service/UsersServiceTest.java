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

    public UsersServiceTest() {
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
            User vo = newVO(editUser);
            User saved = getService().permissionService.runAsSystemAdmin(() -> {
                return service.insert(vo);
            });
            getService().permissionService.runAs(saved, () -> {
                User fromDb = service.get(saved.getId());
                assertVoEqual(saved, fromDb);
            });
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
            User vo = newVO(editUser);
            User saved = getService().permissionService.runAsSystemAdmin(() -> {
                return service.insert(vo);
            });
            getService().permissionService.runAs(vo, () -> {
                saved.setName("I edited myself");
                service.update(saved.getUsername(), saved);
                User updated = service.get(vo.getId());
                assertVoEqual(saved, updated);
            });
        });
    }

    /**
     * Test edit self permission
     */
    @Test
    public void testUserEditRoleDefaultUser() {
        runTest(() -> {
            User vo = newVO(editUser);
            User saved = getService().permissionService.runAsSystemAdmin(() -> {
                return service.insert(vo);
            });

            getService().permissionService.runAs(saved, () -> {
                saved.setName("I edited myself");
                service.update(saved.getUsername(), saved);
                User updated = service.get(saved.getId());
                assertVoEqual(saved, updated);
            });
        });
    }

    @Override
    @Test(expected = PermissionException.class)
    public void testUserEditRoleFails() {
        runTest(() -> {
            addRoleToEditSelfPermission(editRole);
            getService().permissionService.runAs(readUser, () -> {
                User toUpdate = service.get(readUser.getId());
                toUpdate.setName("I edited myself");
                toUpdate.setPassword("");
                service.update(toUpdate.getUsername(), toUpdate);
                User updated = service.get(readUser.getId());
                assertVoEqual(toUpdate, updated);
            });
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
        User vo = newVO(readUser);
        vo.setRoles(Collections.singleton(readRole));
        User saved = getService().permissionService.runAsSystemAdmin(() -> {
            return service.insert(vo);
        });

        getService().permissionService.runAs(saved, () -> {
            saved.setRoles(Collections.singleton(editRole));
            service.update(saved.getUsername(), saved);
        });
    }

    @Test
    @Override
    public void testDeleteRoleUpdateVO() {
        runTest(() -> {
            User vo = newVO(readUser);
            vo.setRoles(Collections.singleton(readRole));
            getService().permissionService.runAsSystemAdmin(() -> {
                service.insert(vo);
                User fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                roleService.delete(readRole.getId());
                //Remove the read role from the local copy
                Set<Role> roles = new HashSet<>(fromDb.getRoles());
                roles.remove(readRole);
                fromDb.setRoles(roles);
                //Check database
                User updated = service.get(fromDb.getId());
                assertVoEqual(fromDb, updated);
                //Check cache
                updated = service.get(fromDb.getUsername());
                assertVoEqual(fromDb, updated);
            });
        });
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            getService().permissionService.runAsSystemAdmin(() -> {
                User vo = insertNewVO(readUser);
                vo.setRoles(Collections.singleton(readRole));
                service.update(vo.getUsername(), vo);
                User fromDb = service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getId());

                //Ensure the mappings are gone
                assertEquals(0, service.getDao().getUserRoles(vo).size());

                service.get(vo.getId());
            });
        });
    }

    @Test(expected = ValidationException.class)
    public void testRemoveRolesFails() {
        User vo = newVO(readUser);
        vo.setRoles(Collections.singleton(readRole));
        User saved = getService().permissionService.runAsSystemAdmin(() -> {
            service.insert(vo);
            return service.get(vo.getId());
        });
        getService().permissionService.runAs(saved, () -> {
            saved.setRoles(Collections.emptySet());
            service.update(saved.getUsername(), saved);
        });
    }

    @Test(expected = ValidationException.class)
    public void testChangeUsername() {
        getService().permissionService.runAsSystemAdmin(() -> {
            User vo = newVO(readUser);
            service.insert(vo);
            vo = service.get(vo.getId());
            vo.setUsername(UUID.randomUUID().toString());
            service.insert(vo);
        });
    }

    void addRoleToEditSelfPermission(Role vo) {
        String permissionType = getCreatePermissionType();
        if(permissionType != null) {
            getService().permissionService.runAsSystemAdmin(() -> {
                roleService.addRoleToPermission(vo, UserEditSelfPermission.PERMISSION);
            });
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
    User newVO(User owner) {
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
        getService().permissionService.runAsSystemAdmin(() -> {
            User user = insertNewVO(readUser);
            assertFalse(service.getDao().isUsernameUnique(user.getUsername(), Common.NEW_ID));
            assertTrue(service.getDao().isUsernameUnique(user.getUsername(), user.getId()));
        });
    }

    @Test
    public void testEmailUnique() {
        getService().permissionService.runAsSystemAdmin(() -> {
            User user = insertNewVO(readUser);
            assertFalse(service.getDao().isEmailUnique(user.getEmail(), Common.NEW_ID));
            assertTrue(service.getDao().isEmailUnique(user.getEmail(), user.getId()));
        });
    }

    @Test
    public void getUserByEmail() {
        getService().permissionService.runAsSystemAdmin(() -> {
            User user = insertNewVO(readUser);
            User dbUser = service.getUserByEmail(user.getEmail());
            assertVoEqual(user, dbUser);
        });
    }

    @Test
    public void getDisabledUsers() {
        getService().permissionService.runAsSystemAdmin(() -> {
            User user = insertNewVO(readUser);
            user.setDisabled(true);
            service.update(user.getId(), user);
            List<User> active = service.getDao().getActiveUsers();
            List<User> all = service.getDao().getAll();
            assertEquals(all.size() - 1, active.size());
        });
    }

    @Override
    void addReadRoleToFail(Role role, User vo) {
        vo.getRoles().add(role);
    }

    @Override
    void addEditRoleToFail(Role role, User vo) {
        throw new UnsupportedOperationException();
    }
}
