/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.db.tables.records.UsersRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.rules.ExpectValidationException;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.ChangeOwnUsernamePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.UserCreatePermission;
import com.serotonin.m2m2.module.definitions.permissions.UserEditSelfPermission;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;

/**
 * @author Terry Packer
 *
 */
public class UsersServiceTest extends AbstractVOServiceWithPermissionsTest<User, UsersRecord, Users, UserDao, UsersService> {

    /**
     * Test that we can read ourself
     */
    @Test
    @Override
    public void testUserReadRole() {
        User vo = newVO(editUser);
        User saved = service.insert(vo);
        runAs.runAs(saved, () -> {
            User fromDb = service.get(saved.getId());
            assertVoEqual(saved, fromDb);
        });
    }

    @Test
    @Override
    public void testCreatePrivilegeSuccess() {
        User vo = newVO(editUser);
        addRoleToCreatePermission(editRole);
        vo.setRoles(Collections.singleton(editRole));
        vo.setReadPermission(MangoPermission.requireAnyRole(editRole));
        vo.setEditPermission(MangoPermission.requireAnyRole(editRole));
        runAs.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    /**
     * Test edit self permission
     */
    @Override
    @Test
    public void testUserEditRole() {
        setEditSelfPermission(MangoPermission.requireAnyRole(readRole));
        User vo = newVO(editUser);
        vo.setRoles(Collections.singleton(readRole));
        User saved = service.insert(vo);
        runAs.runAs(vo, () -> {
            saved.setName("I edited myself");
            service.update(saved.getUsername(), saved);
            User updated = service.get(vo.getId());
            assertVoEqual(saved, updated);
        });
    }

    @Test
    public void testUserEditRoleDefaultUser() {
        User vo = newVO(editUser);
        User saved = service.insert(vo);

        //Ensure the ability to edit self
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));

        runAs.runAs(saved, () -> {
            saved.setName("I edited myself");
            service.update(saved.getUsername(), saved);
            User updated = service.get(saved.getId());
            assertVoEqual(saved, updated);
        });
    }

    @Override
    @Test(expected = PermissionException.class)
    public void testUserEditRoleFails() {
        setEditSelfPermission(MangoPermission.requireAnyRole(editRole));
        runAs.runAs(readUser, () -> {
            User toUpdate = service.get(readUser.getId());
            toUpdate.setName("I edited myself");
            toUpdate.setPassword("");
            service.update(toUpdate.getUsername(), toUpdate);
            User updated = service.get(readUser.getId());
            assertVoEqual(toUpdate, updated);
        });
    }

    @Test
    @Override
    @ExpectValidationException("roles")
    public void testAddEditRoleUserDoesNotHave() {
        User vo = newVO(readUser);
        vo.setRoles(Collections.singleton(readRole));
        service.insert(vo);

        //Ensure the ability to edit self
        setEditSelfPermission(MangoPermission.requireAnyRole(readRole));

        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            Set<Role> newRoles = new HashSet<>(self.getRoles());
            newRoles.add(editRole);
            self.setRoles(newRoles);
            service.update(self.getId(), self);
        });
    }

    @Test
    @Override
    public void testDeleteRoleUpdateVO() {
        User vo = newVO(readUser);
        vo.setRoles(Collections.singleton(readRole));
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
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        User vo = insertNewVO(readUser);
        vo.setRoles(Collections.singleton(readRole));
        service.update(vo.getUsername(), vo);
        User fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        service.delete(vo.getId());

        //Ensure the mappings are gone
        assertEquals(0, getDao().getUserRoles(vo).size());

        service.get(vo.getId());
    }

    @Test
    @ExpectValidationException("roles")
    public void testRemoveRolesFails() {
        User vo = newVO(null);
        vo.setRoles(Collections.singleton(readRole));
        service.insert(vo);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setRoles(Collections.emptySet());
            service.update(self.getId(), self);
        });
    }

    @Test
    public void testChangeUsernameAsAdmin() {
        User vo = newVO(readUser);
        service.insert(vo);
        vo = service.get(vo.getId());
        vo.setUsername(randomXid());
        service.update(vo.getId(), vo);
    }

    @Test
    @ExpectValidationException("username")
    public void testChangeUsernameWithoutPermission() {
        setEditSelfPermission(MangoPermission.requireAnyRole(readRole));
        PermissionDefinition def = ModuleRegistry.getPermissionDefinition(ChangeOwnUsernamePermissionDefinition.PERMISSION);

        Set<Set<Role>> roleSet = def.getPermission().getRoles();
        Set<Set<Role>> newRoles = new HashSet<>();
        newRoles.add(Collections.singleton(editRole));
        for (Set<Role> roles : roleSet) {
            if (roles.contains(PermissionHolder.USER_ROLE)) {
                continue; //skip the user role
            }
            newRoles.add(roles);
        }
        systemPermissionService.update(new MangoPermission(newRoles), def);

        User vo = newVO(readUser);
        vo.setRoles(Collections.singleton(readRole));
        service.insert(vo);
        User saved = service.get(vo.getId());

        runAs.runAs(saved, () -> {
            saved.setUsername(randomXid());
            service.update(saved.getId(), saved);
        });
    }

    @Test
    public void testChangeUsernameWithPermission() {

        //Add read role to change username permission
        PermissionDefinition def = ModuleRegistry.getPermissionDefinition(ChangeOwnUsernamePermissionDefinition.PERMISSION);
        Set<Set<Role>> roleSet = def.getPermission().getRoles();
        Set<Set<Role>> newRoles = new HashSet<>();
        newRoles.add(Collections.singleton(readRole));
        for (Set<Role> roles : roleSet) {
            newRoles.add(new HashSet<>(roles));
        }
        systemPermissionService.update(new MangoPermission(newRoles), def);

        //Ensure they can edit self
        setEditSelfPermission(MangoPermission.requireAnyRole(readRole));

        User vo = newVO(readUser);
        vo.setRoles(Collections.singleton(readRole));
        service.insert(vo);
        User saved = service.get(vo.getId());

        runAs.runAs(saved, () -> {
            saved.setUsername(randomXid());
            service.update(saved.getId(), saved);
        });
    }

    void setEditSelfPermission(MangoPermission permission) {
        systemPermissionService.update(permission, Common.getBean(UserEditSelfPermission.class));
    }

    public Role createUsersRole() {
        Role createRole = createRole(randomXid(), "Create users role").getRole();
        PermissionDefinition createPermission = Common.getBean(UserCreatePermission.class);
        systemPermissionService.update(MangoPermission.requireAnyRole(createRole), createPermission);
        return createRole;
    }

    @Override
    String getCreatePermissionType() {
        return UserCreatePermission.PERMISSION;
    }

    @Override
    void setReadPermission(MangoPermission permission, User vo) {
        vo.setReadPermission(permission);
    }

    @Override
    void setEditPermission(MangoPermission permission, User vo) {
        vo.setEditPermission(permission);
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
        assertEquals(expected.getEmailVerifiedDate(), actual.getEmailVerifiedDate());
        assertEquals(expected.getData(), actual.getData());
        assertRoles(expected.getRoles(), actual.getRoles());

    }

    public User insertUser(Role... roles) {
        User user = newVO(null);
        user.setReadPermission(MangoPermission.requireAnyRole(readRole));
        user.setEditPermission(MangoPermission.requireAnyRole(editRole));
        user.setRoles(Arrays.stream(roles).collect(Collectors.toSet()));
        return service.insert(user);
    }

    @Override
    User newVO(User owner) {
        User user = new User();
        user.setName("usersServiceTest");
        user.setUsername(randomXid());
        user.setPassword(Common.encrypt("usersServiceTest"));
        user.setEmail(randomXid() + "@example.com");
        user.setPhone("");
        user.setDisabled(false);
        return user;
    }

    @Override
    User updateVO(User existing) {
        existing.setName("usersServiceTest2");
        existing.setPassword(Common.encrypt("usersServiceTest2"));
        existing.setEmail(randomXid() + "@example.com");
        existing.setPhone("");
        existing.setDisabled(false);
        return existing;
    }

    @Test
    public void testUsernameUnique() {
        User user = insertNewVO(readUser);
        assertFalse(getDao().isUsernameUnique(user.getUsername(), Common.NEW_ID));
        assertTrue(getDao().isUsernameUnique(user.getUsername(), user.getId()));
    }

    @Test
    public void testEmailUnique() {
        User user = insertNewVO(readUser);
        assertFalse(getDao().isEmailUnique(user.getEmail(), Common.NEW_ID));
        assertTrue(getDao().isEmailUnique(user.getEmail(), user.getId()));
    }

    @Test
    public void getUserByEmail() {
        User user = insertNewVO(readUser);
        User dbUser = service.getUserByEmail(user.getEmail());
        assertVoEqual(user, dbUser);
    }

    @Test
    public void getDisabledUsers() {
        User user = insertNewVO(readUser);
        user.setDisabled(true);
        service.update(user.getId(), user);
        List<User> active = getDao().getActiveUsers();
        List<User> all = getDao().getAll();
        assertEquals(all.size() - 1, active.size());
    }

    @Override
    String getReadPermissionContextKey() {
        return "readPermission";
    }

    @Override
    String getEditPermissionContextKey() {
        return "editPermission";
    }

    /**
     * Roles
     */

    @Test
    public void userRoleIsAlwaysAdded() {
        User vo = newVO(null);
        vo.setRoles(Collections.emptySet());
        service.insert(vo);
        assertThat(vo.getRoles(), hasItem(PermissionHolder.USER_ROLE));
    }

    @Test
    @ExpectValidationException("roles")
    public void cantAddRolesToSelf() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = newVO(null);
        vo.setRoles(Collections.singleton(editRole));
        vo.setEditPermission(MangoPermission.superadminOnly());
        service.insert(vo);

        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setRoles(Stream.concat(
                    self.getRoles().stream(),
                    Stream.of(readRole)
            ).collect(Collectors.toSet()));
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException("roles")
    public void cantAddRolesToSelfWithExplicitEditPermission() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = insertUser(editRole);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setRoles(Stream.concat(
                self.getRoles().stream(),
                Stream.of(readRole)
            ).collect(Collectors.toSet()));
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException("roles")
    public void cantRemoveRolesFromSelf() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = newVO(null);
        vo.setRoles(new HashSet<>(Arrays.asList(editRole, readRole)));
        vo.setEditPermission(MangoPermission.superadminOnly());
        service.insert(vo);

        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setRoles(self.getRoles().stream()
                    .filter(r -> !r.equals(readRole))
                    .collect(Collectors.toSet()));
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException("roles")
    public void cantRemoveRolesFromSelfWithExplicitEditPermission() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = insertUser(editRole, readRole);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setRoles(self.getRoles().stream()
                    .filter(r -> !r.equals(readRole))
                    .collect(Collectors.toSet()));
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException("roles")
    public void superadminCantRemoveSuperadminRoleFromSelf() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = insertUser(editRole, readRole, PermissionHolder.SUPERADMIN_ROLE);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setRoles(self.getRoles().stream()
                    .filter(r -> !r.equals(PermissionHolder.SUPERADMIN_ROLE))
                    .collect(Collectors.toSet()));
            service.update(self.getId(), self);
        });
    }

    @Test
    public void superadminCanRemoveOtherRoleFromSelf() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = insertUser(editRole, readRole, PermissionHolder.SUPERADMIN_ROLE);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setRoles(self.getRoles().stream()
                    .filter(r -> !r.equals(readRole))
                    .collect(Collectors.toSet()));
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException("roles")
    public void cantAddAdditionalRolesWhenCreating() {
        Role roleToAdd = createRole(randomXid(), "Some other role").getRole();
        Role createRole = createUsersRole();
        User createUser = insertUser(createRole, editRole, readRole);
        runAs.runAs(createUser, () -> {
            insertUser(roleToAdd);
        });
    }

    @Test
    @ExpectValidationException("roles")
    public void cantAddAdditionalRolesWhenUpdating() {
        Role roleToAdd = createRole(randomXid(), "Some other role").getRole();
        Role createRole = createUsersRole();
        User createUser = insertUser(createRole, editRole, readRole);
        User otherUser = insertUser();
        runAs.runAs(createUser, () -> {
            otherUser.setRoles(Collections.singleton(roleToAdd));
            service.update(otherUser.getId(), otherUser);
        });
    }

    @Test
    public void canAddOwnRolesWhenCreating() {
        Role createRole = createUsersRole();
        User createUser = insertUser(createRole, editRole, readRole);
        runAs.runAs(createUser, () -> {
            insertUser(readRole);
        });
    }

    @Test
    public void canAddOwnRolesWhenUpdating() {
        Role createRole = createUsersRole();
        User createUser = insertUser(createRole, editRole, readRole);
        User otherUser = insertUser();
        runAs.runAs(createUser, () -> {
            otherUser.setRoles(Collections.singleton(readRole));
            service.update(otherUser.getId(), otherUser);
        });
    }

    @Test
    public void superadminCanAddRolesWhenCreating() {
        User createUser = insertUser(PermissionHolder.SUPERADMIN_ROLE);
        runAs.runAs(createUser, () -> {
            insertUser(readRole);
        });
    }

    @Test
    public void superadminCanAddRolesWhenUpdating() {
        User createUser = insertUser(PermissionHolder.SUPERADMIN_ROLE);
        User otherUser = insertNewVO(null);
        runAs.runAs(createUser, () -> {
            otherUser.setRoles(Collections.singleton(readRole));
            service.update(otherUser.getId(), otherUser);
        });
    }

    /**
     * Permissions
     */

    @Test(expected = PermissionException.class)
    public void cantCreateWithoutPermission() {
        User createUser = insertNewVO(null);
        runAs.runAs(createUser, () -> {
            insertNewVO(null);
        });
    }

    @Test
    @ExpectValidationException("readPermission")
    public void cantCreateUserYouCantRead() {
        Role createRole = createUsersRole();
        User createUser = insertUser(editRole, createRole);
        //noinspection Convert2MethodRef
        runAs.runAs(createUser, () -> insertUser());
    }

    @Test
    @ExpectValidationException("editPermission")
    public void cantCreateUserYouCantEdit() {
        Role createRole = createUsersRole();
        User createUser = insertUser(readRole, createRole);
        //noinspection Convert2MethodRef
        runAs.runAs(createUser, () -> insertUser());
    }


    @Test
    public void canChangeOwnNameWithEditSelfPermission() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = insertUser();
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setName("new name!");
            service.update(self.getId(), self);
        });
    }

    @Test(expected = PermissionException.class)
    public void cantChangeOwnNameWithoutEditSelfPermission() {
        setEditSelfPermission(MangoPermission.superadminOnly());
        User vo = insertUser();
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setName("new name!");
            service.update(self.getId(), self);
        });
    }

    @Test
    public void canChangeOwnNameWithExplicitPermissionOnly() {
        setEditSelfPermission(MangoPermission.superadminOnly());
        User vo = insertUser(editRole);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setName("new name!");
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException("disabled")
    public void cantDisableSelf() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = insertUser();
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setDisabled(true);
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException("disabled")
    public void cantDisableSelfEvenWithExplicitEditPermission() {
        User vo = insertUser(editRole, readRole);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setDisabled(true);
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException("disabled")
    public void cantDisableSelfEvenIfSuperadmin() {
        User vo = insertUser(PermissionHolder.SUPERADMIN_ROLE);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setDisabled(true);
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException({"readPermission", "editPermission"})
    public void cantChangePermissionsOfSelf() {
        setEditSelfPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
        User vo = insertUser();
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setReadPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
            self.setEditPermission(MangoPermission.requireAnyRole(PermissionHolder.USER_ROLE));
            service.update(self.getId(), self);
        });
    }

    @Test
    public void canChangePermissionsOfSelfWithExplicitPermission() {
        Role otherRole = createRole(randomXid(), "Some other role").getRole();
        User vo = insertUser(editRole, readRole, otherRole);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setReadPermission(MangoPermission.requireAnyRole(otherRole));
            self.setEditPermission(MangoPermission.requireAnyRole(otherRole));
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException({"readPermission", "editPermission"})
    public void mustRetainAccessToSelf() {
        User vo = insertUser(editRole, readRole);
        runAs.runAs(vo, () -> {
            User self = service.get(vo.getId());
            self.setReadPermission(MangoPermission.superadminOnly());
            self.setEditPermission(MangoPermission.superadminOnly());
            service.update(self.getId(), self);
        });
    }

    @Test
    public void canEditPermissions() {
        Role otherRole = createRole(randomXid(), "Some other role").getRole();
        User otherUser = insertUser();
        User user = insertUser(editRole, readRole, otherRole);
        runAs.runAs(user, () -> {
            otherUser.setReadPermission(MangoPermission.requireAnyRole(otherRole));
            otherUser.setEditPermission(MangoPermission.requireAnyRole(otherRole));
            service.update(otherUser.getId(), otherUser);
        });
    }

    @Test
    @ExpectValidationException({"readPermission", "editPermission"})
    public void cannotAllowAccessToRoleWeDontHold() {
        Role otherRole = createRole(randomXid(), "Some other role").getRole();
        User otherUser = insertUser();
        User user = insertUser(editRole, readRole);
        runAs.runAs(user, () -> {
            otherUser.setReadPermission(MangoPermission.requireAnyRole(readRole, otherRole));
            otherUser.setEditPermission(MangoPermission.requireAnyRole(editRole, otherRole));
            service.update(otherUser.getId(), otherUser);
        });
    }

    @Test
    @ExpectValidationException({"readPermission", "editPermission"})
    public void mustRetainAccess() {
        Role otherRole = createRole(randomXid(), "Some other role").getRole();
        User otherUser = insertUser();
        User user = insertUser(editRole, readRole);
        runAs.runAs(user, () -> {
            otherUser.setReadPermission(MangoPermission.requireAnyRole(otherRole));
            otherUser.setEditPermission(MangoPermission.requireAnyRole(otherRole));
            service.update(otherUser.getId(), otherUser);
        });
    }

    @Test
    @ExpectValidationException("created")
    public void cantChangeCreatedTimeOfSelf() {
        User user = insertUser();
        runAs.runAs(user, () -> {
            User self = service.get(user.getId());
            self.setCreated(new Date());
            service.update(self.getId(), self);
        });
    }

    @Test
    public void canChangeCreatedTimeOfSelfWithExplicitPermission() {
        User user = insertUser(editRole);
        runAs.runAs(user, () -> {
            User self = service.get(user.getId());
            self.setCreated(new Date());
            service.update(self.getId(), self);
        });
    }

    @Test
    @ExpectValidationException({"sessionExpirationOverride", "sessionExpirationPeriods", "sessionExpirationPeriodType"})
    public void cantChangeSessionExpirationOfSelf() {
        User user = insertUser();
        runAs.runAs(user, () -> {
            User self = service.get(user.getId());
            self.setSessionExpirationOverride(true);
            self.setSessionExpirationPeriods(5);
            self.setSessionExpirationPeriodType("MINUTES");
            service.update(self.getId(), self);
        });
    }

    @Test
    public void canChangeSessionExpirationOfSelfWithExplicitPermission() {
        User user = insertUser(editRole);
        runAs.runAs(user, () -> {
            User self = service.get(user.getId());
            self.setSessionExpirationOverride(true);
            self.setSessionExpirationPeriods(5);
            self.setSessionExpirationPeriodType("MINUTES");
            service.update(self.getId(), self);
        });
    }

}
