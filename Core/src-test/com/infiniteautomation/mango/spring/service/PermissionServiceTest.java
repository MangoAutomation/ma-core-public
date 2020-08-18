/*
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import com.google.common.collect.Sets;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Terry Packer
 *
 */
public class PermissionServiceTest extends MangoTestBase {

    protected RoleService roleService;
    protected PermissionService permissionService;
    protected DataSourceService dataSourceService;
    protected DataPointService dataPointService;
    protected UsersService usersService;

    protected PermissionHolder systemSuperadmin;

    public PermissionServiceTest() {
    }

    @Before
    public void setupRoles() {
        roleService = Common.getBean(RoleService.class);
        permissionService = Common.getBean(PermissionService.class);
        dataSourceService = Common.getBean(DataSourceService.class);
        dataPointService = Common.getBean(DataPointService.class);
        usersService = Common.getBean(UsersService.class);
        systemSuperadmin = PermissionHolder.SYSTEM_SUPERADMIN;
    }

    MockDataSourceVO createDataSource(Role editRole) {
        return createDataSource(Collections.singleton(editRole));
    }

    MockDataSourceVO createDataSource(Set<Role> editRoles) {
        MockDataSourceVO dsVo = new MockDataSourceVO();
        dsVo.setName("permissions_test_datasource");
        dsVo.setEditPermission(MangoPermission.requireAnyRole(editRoles));
        return (MockDataSourceVO) permissionService.runAsSystemAdmin(() -> dataSourceService.insert(dsVo));
    }

    DataPointVO createDataPoint() {
        return createDataPoint(Collections.emptySet(), Collections.emptySet());
    }

    DataPointVO createDataPoint(DataSourceVO ds) {
        return createDataPoint(ds, Collections.emptySet(), Collections.emptySet());
    }

    DataPointVO createDataPoint(Set<Role> readRoles, Set<Role> setRoles) {
        MockDataSourceVO ds = createDataSource(Collections.emptySet());
        return createDataPoint(ds, readRoles, setRoles);
    }

    DataPointVO createDataPoint(DataSourceVO dsVo, Set<Role> readRoles, Set<Role> setRoles) {
        DataPointVO point = new DataPointVO();
        point.setDataSourceId(dsVo.getId());
        point.setName("permissions_test_datasource");
        point.setReadPermission(MangoPermission.requireAnyRole(readRoles));
        point.setSetPermission(MangoPermission.requireAnyRole(setRoles));
        point.setPointLocator(new MockPointLocatorVO());
        return permissionService.runAsSystemAdmin(() -> {
            dataPointService.insert(point);
            return point;
        });
    }

    Role randomRole() {
        RoleVO vo = new RoleVO(Common.NEW_ID, UUID.randomUUID().toString(), "Random permission");
        return permissionService.runAsSystemAdmin(() -> {
            roleService.insert(vo);
            return new Role(vo);
        });
    }

    Set<Role> randomRoles(int size) {
        Set<Role> roles = new HashSet<>();
        for(int i=0; i<size; i++) {
            roles.add(randomRole());
        }
        return roles;
    }

    User createTestUser() {
        return createTestUser(randomRole());
    }

    User createTestUser(Role role) {
        return createTestUser(Collections.singleton(role));
    }

    User createTestUserWithAllRolesFromPermission(MangoPermission permission) {
        return createTestUser(permission.getRoles().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
    }

    User createTestUser(Set<Role> roles) {
        return permissionService.runAsSystemAdmin(() -> createUser("permissions_test_user",
                "permissions_test_user",
                "permissions_test_user",
                "permissions_test_user@test.com",
                roles.toArray(new Role[0])));
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setRuntimeManager(new MockRuntimeManager(true));
        return lifecycle;
    }

    @Test
    public void ensureValidPermissionHolderOK() {
        User testUser = this.createTestUser();
        permissionService.ensureValidPermissionHolder(testUser);
    }

    @Test(expected = PermissionException.class)
    public void ensureValidPermissionHolderNull() {
        permissionService.ensureValidPermissionHolder(null);
    }

    @Test(expected = PermissionException.class)
    public void ensureValidPermissionHolderDisabled() {
        User testUser = this.createTestUser();
        testUser.setDisabled(true);
        permissionService.ensureValidPermissionHolder(testUser);
    }

    @Test
    public void isValidPermissionHolderOK() {
        User testUser = this.createTestUser();
        assertTrue(permissionService.isValidPermissionHolder(testUser));
    }

    @Test
    public void isValidPermissionHolderFailNull() {
        assertFalse(permissionService.isValidPermissionHolder(null));
    }

    @Test
    public void isValidPermissionHolderFailDisabled() {
        User testUser = this.createTestUser();
        testUser.setDisabled(true);
        assertFalse(permissionService.isValidPermissionHolder(testUser));
    }

    @Test
    public void ensureAdminRoleOK() {
        User testUser = this.createTestUser();
        testUser.setRoles(Collections.singleton(roleService.getSuperadminRole()));
        permissionService.runAsSystemAdmin(() -> {
            usersService.update(testUser.getUsername(), testUser);
            permissionService.ensureAdminRole(testUser);
        });
    }

    @Test(expected = PermissionException.class)
    public void ensureAdminPermissionFail() {
        User testUser = this.createTestUser();
        permissionService.ensureAdminRole(testUser);
    }

    @Test
    public void ensureDataSourcePermissionOK() {
        User testUser = this.createTestUser();
        testUser.setRoles(Collections.singleton(roleService.getSuperadminRole()));
        permissionService.runAsSystemAdmin(() -> {
            usersService.update(testUser.getUsername(), testUser);
            permissionService.ensureDataSourcePermission(testUser);
        });
    }

    @Test(expected = PermissionException.class)
    public void ensureDataSourcePermissionFail() {
        User testUser = this.createTestUser();
        permissionService.ensureDataSourcePermission(testUser);
    }

    @Test
    public void ensureDataSourcePermissionDsVoOK() {
        User testUser = this.createTestUser();
        MockDataSourceVO ds = createDataSource(testUser.getRoles());
        permissionService.ensurePermission(testUser, ds.getEditPermission());
    }

    @Test(expected = PermissionException.class)
    public void ensureDataSourcePermissionDsVoFail() {
        User testUser = this.createTestUser();
        MockDataSourceVO ds = createDataSource(randomRoles(1));
        permissionService.ensurePermission(testUser, ds.getEditPermission());
    }

    @Test
    public void ensureDataPointReadPermissionOK() {
        User testUser = this.createTestUser();
        DataPointVO dp = createDataPoint(testUser.getRoles(), Collections.emptySet());
        permissionService.ensurePermission(testUser, dp.getReadPermission());
    }

    @Test(expected = PermissionException.class)
    public void ensureDataPointReadPermissionFail() {
        User testUser = this.createTestUser();
        DataPointVO dp = createDataPoint();
        permissionService.ensurePermission(testUser, dp.getReadPermission());
    }

    @Test
    public void ensureDataPointReadPermissionOkEmptyPerms() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        DataPointVO dp = createDataPoint();
        permissionService.ensurePermission(testUser, dp.getReadPermission());
    }

    @Test(expected = PermissionException.class)
    public void ensureDataPointReadPermissionFailEmptyPerms() {
        User testUser = this.createTestUser();
        DataPointVO dp = createDataPoint();
        permissionService.ensurePermission(testUser, dp.getReadPermission());
    }

    @Test(expected = PermissionException.class)
    public void ensureNoDataPointReadPermissionWithDataSourcePermission() {
        MockDataSourceVO ds = createDataSource(randomRole());
        DataPointVO dp = createDataPoint(ds);
        User testUser = createTestUserWithAllRolesFromPermission(ds.getEditPermission());
        permissionService.ensurePermission(testUser, dp.getReadPermission());
    }

    @Test(expected = PermissionException.class)
    public void ensureNoDataPointReadPermissionWithSetPermission() {
        DataPointVO dp = createDataPoint(Collections.emptySet(), randomRoles(1));
        User testUser = createTestUserWithAllRolesFromPermission(dp.getSetPermission());
        permissionService.ensurePermission(testUser, dp.getReadPermission());
    }

    @Test
    public void ensureDataPointReadPermissionOKHasReadPermission() {
        DataPointVO dp = createDataPoint(randomRoles(1), Collections.emptySet());
        User testUser = createTestUserWithAllRolesFromPermission(dp.getReadPermission());
        permissionService.ensurePermission(testUser, dp.getReadPermission());
    }

    @Test(expected = PermissionException.class)
    public void ensureNoDataPointSetPermissionWithDataSourcePermission() {
        MockDataSourceVO ds = createDataSource(randomRoles(1));
        User testUser = createTestUserWithAllRolesFromPermission(ds.getEditPermission());
        DataPointVO dp = createDataPoint(ds);
        permissionService.ensurePermission(testUser, dp.getSetPermission());
    }

    @Test
    public void ensureDataPointSetPermissionOKHasSetPermission() {
        DataPointVO dp = createDataPoint(Collections.emptySet(), randomRoles(1));
        User testUser = createTestUserWithAllRolesFromPermission(dp.getSetPermission());
        permissionService.ensurePermission(testUser, dp.getSetPermission());
    }

    @Test(expected = PermissionException.class)
    public void ensureDataPointSetPermissionFailHasReadPermission() {
        DataPointVO dp = createDataPoint(randomRoles(1), randomRoles(1));
        User testUser = createTestUserWithAllRolesFromPermission(dp.getReadPermission());
        permissionService.ensurePermission(testUser, dp.getSetPermission());
    }

    @Test
    public void implodeRoles() {
        Role perm1 = this.randomRole();
        Role perm2 = this.randomRole();

        Set<Role> permSet = new HashSet<>();
        permSet.add(perm1);
        permSet.add(perm2);

        String joinedPerms = PermissionService.implodeRoles(permSet);

        assertTrue(joinedPerms.contains(perm1.getXid()));
        assertTrue(joinedPerms.contains(perm2.getXid()));
        assertTrue(joinedPerms.contains(","));
        assertFalse(joinedPerms.contains(" "));
    }

    @Test
    public void ensureHasSingleRoleOKSuperadmin() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        for (Role role : testUser.getRoles()) {
            permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole(role));
        }
    }

    @Test
    public void ensureHasSingleRoleOKHasPerm() {
        Set<Role> roles = this.randomRoles(2);
        User testUser = this.createTestUser(roles);
        for (Role role : testUser.getRoles()) {
            permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole(role));
        }
    }

    @Test(expected = PermissionException.class)
    public void ensureHasSingleRoleFailEmptyRoles() {
        User testUser = this.createTestUser();
        Role unsavedRole = new Role(Common.NEW_ID, UUID.randomUUID().toString());
        permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole(unsavedRole));
    }

    @Test
    public void ensureHasAnyRoleOKSuperadmin() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole(randomRoles(2)));
    }

    @Test
    public void ensureHasAnyRoleOKSuperadminEmptySet() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole());
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAnyPermissionFailEmptySet() {
        User testUser = this.createTestUser();
        permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole());
    }

    @Test
    public void ensureHasAnyRoleOKHasOne() {
        Role perm1 = this.randomRole();
        Role perm2 = this.randomRole();
        Role perm3 = this.randomRole();

        User testUser = this.createTestUser(Sets.newHashSet(perm1, perm2));

        permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole(perm1, perm3));
    }

    @Test
    public void ensureHasAnyRoleOKHasBoth() {
        Role perm1 = this.randomRole();
        Role perm2 = this.randomRole();

        User testUser = this.createTestUser(Sets.newHashSet(perm1, perm2));

        permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole(perm1, perm2));
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAnyPermissionFailHasNeither() {
        Role perm1 = this.randomRole();
        Role perm2 = this.randomRole();
        Role perm3 = this.randomRole();
        Role perm4 = this.randomRole();

        User testUser = this.createTestUser(Sets.newHashSet(perm1, perm2));

        permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole(perm3, perm4));
    }

    @Test(expected = NullPointerException.class)
    public void ensureHasAnyPermissionFailNullSet() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        permissionService.ensurePermission(testUser, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureHasAnyPermissionFailNullEntry() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        permissionService.ensurePermission(testUser, MangoPermission.requireAnyRole((Role) null));
    }

    @Test
    public void ensureHasAllPermissionsOKSuperadmin() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        permissionService.ensurePermission(testUser, MangoPermission.requireAllRoles(randomRole(), randomRole()));
    }

    @Test
    public void ensureHasAllPermissionsOKSuperadminEmptySet() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        permissionService.ensurePermission(testUser, MangoPermission.requireAllRoles());
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAllPermissionsFailEmptySet() {
        User testUser = this.createTestUser();
        permissionService.ensurePermission(testUser, MangoPermission.requireAllRoles());
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAllPermissionsFailHasOne() {
        Role perm1 = this.randomRole();
        Role perm2 = this.randomRole();

        User testUser = this.createTestUser(Sets.newHashSet(perm1, randomRole()));
        permissionService.ensurePermission(testUser, MangoPermission.requireAllRoles(perm1, perm2));
    }

    @Test
    public void ensureHasAllPermissionsOKHasBoth() {
        Role perm1 = this.randomRole();
        Role perm2 = this.randomRole();

        User testUser = this.createTestUser(Sets.newHashSet(perm1, perm2));
        permissionService.ensurePermission(testUser, MangoPermission.requireAllRoles(perm1, perm2));
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAllPermissionsFailHasNeither() {
        Role perm1 = this.randomRole();
        Role  perm2 = this.randomRole();

        User testUser = this.createTestUser(Sets.newHashSet(randomRole(), randomRole()));
        permissionService.ensurePermission(testUser, MangoPermission.requireAllRoles(perm1, perm2));
    }

    @Test(expected = NullPointerException.class)
    public void ensureHasAllPermissionsFailNullSet() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        permissionService.ensurePermission(testUser, MangoPermission.requireAllRoles((Role) null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureHasAllPermissionsFailNullEntry() {
        User testUser = this.createTestUser(roleService.getSuperadminRole());
        permissionService.ensurePermission(testUser, MangoPermission.requireAllRoles((Role) null));
    }
}
