/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.permissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.util.BackgroundContext;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Jared Wiltshire
 */
public class PermissionsTest extends MangoTestBase {

    private static final String SUPERADMIN = SuperadminPermissionDefinition.GROUP_NAME;

    DataSourceVO<?> dataSource;
    DataPointVO dataPoint;

    public PermissionsTest() {
        super(true, 8000);
    }
    
    User createTestUser() {
        User user = new User();
        user.setId(Common.NEW_ID);
        user.setName("permissions_test_user");
        user.setUsername("permissions_test_user");
        user.setPassword(Common.encrypt("permissions_test_user"));
        user.setEmail("permissions_test_user@test.com");
        user.setPhone("");
        user.setPermissions(this.randomPermission());
        user.setDisabled(false);
        user.ensureValid();
        return user;
    }

    String randomPermission() {
        return UUID.randomUUID().toString();
    }

    DataSourceVO<?> createDataSource(String editPermission) {
        MockDataSourceVO dsVo = new MockDataSourceVO();
        dsVo.setName("permissions_test_datasource");
        dsVo.setEditPermission(editPermission);
        DataSourceDao.getInstance().save(dsVo);
        return dsVo;
    }

    DataPointVO createDataPoint(DataSourceVO<?> dsVo, String readPermission, String setPermission) {
        DataPointVO point = new DataPointVO();
        point.setDataSourceId(dsVo.getId());
        point.setName("permissions_test_datasource");
        point.setReadPermission(readPermission);
        point.setSetPermission(setPermission);
        point.setPointLocator(new MockPointLocatorVO());
        DataPointDao.getInstance().save(point);
        return point;
    }

    @Before
    public void setup() {
        this.dataSource = this.createDataSource(this.randomPermission());
        this.dataPoint = this.createDataPoint(this.dataSource, this.randomPermission(), this.randomPermission());
    }

    @Test
    public void ensureValidPermissionHolderOK() {
        User testUser = this.createTestUser();
        Permissions.ensureValidPermissionHolder(testUser);
    }

    @Test(expected = PermissionException.class)
    public void ensureValidPermissionHolderNull() {
        Permissions.ensureValidPermissionHolder(null);
    }

    @Test(expected = PermissionException.class)
    public void ensureValidPermissionHolderDisabled() {
        User testUser = this.createTestUser();
        testUser.setDisabled(true);
        Permissions.ensureValidPermissionHolder(testUser);
    }

    @Test
    public void isValidPermissionHolderOK() {
        User testUser = this.createTestUser();
        assertTrue(Permissions.isValidPermissionHolder(testUser));
    }

    @Test
    public void isValidPermissionHolderFailNull() {
        assertFalse(Permissions.isValidPermissionHolder(null));
    }

    @Test
    public void isValidPermissionHolderFailDisabled() {
        User testUser = this.createTestUser();
        testUser.setDisabled(true);
        assertFalse(Permissions.isValidPermissionHolder(testUser));
    }

    @Test
    public void ensureHasAdminPermissionOK() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAdminPermission(testUser);
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAdminPermissionFail() {
        User testUser = this.createTestUser();
        Permissions.ensureHasAdminPermission(testUser);
    }

    @Test
    public void ensureDataSourcePermissionOK() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureDataSourcePermission(testUser);
    }

    @Test(expected = PermissionException.class)
    public void ensureDataSourcePermissionFail() {
        User testUser = this.createTestUser();
        Permissions.ensureDataSourcePermission(testUser);
    }

    @Test
    public void ensureDataSourcePermissionDsVoOK() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureDataSourcePermission(testUser, this.dataSource);
    }

    @Test(expected = PermissionException.class)
    public void ensureDataSourcePermissionDsVoFail() {
        User testUser = this.createTestUser();
        Permissions.ensureDataSourcePermission(testUser, this.dataSource);
    }

    @Test
    public void ensureDataPointReadPermissionOK() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test(expected = PermissionException.class)
    public void ensureDataPointReadPermissionFail() {
        User testUser = this.createTestUser();
        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test
    public void ensureDataPointReadPermissionOkEmptyPerms() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);

        this.dataSource = this.createDataSource("");
        this.dataPoint = this.createDataPoint(this.dataSource, "", "");

        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test(expected = PermissionException.class)
    public void ensureDataPointReadPermissionFailEmptyPerms() {
        User testUser = this.createTestUser();

        this.dataSource = this.createDataSource("");
        this.dataPoint = this.createDataPoint(this.dataSource, "", "");

        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test
    public void ensureDataPointReadPermissionOkNullPerms() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);

        this.dataSource = this.createDataSource(null);
        this.dataPoint = this.createDataPoint(this.dataSource, null, null);

        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test(expected = PermissionException.class)
    public void ensureDataPointReadPermissionFailNullPerms() {
        User testUser = this.createTestUser();

        this.dataSource = this.createDataSource(null);
        this.dataPoint = this.createDataPoint(this.dataSource, null, null);

        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test
    public void ensureDataPointReadPermissionOKHasDataSourcePermission() {
        User testUser = this.createTestUser();
        testUser.setPermissions(this.dataSource.getEditPermission());
        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test
    public void ensureDataPointReadPermissionOKHasSetPermission() {
        User testUser = this.createTestUser();
        testUser.setPermissions(this.dataPoint.getSetPermission());
        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test
    public void ensureDataPointReadPermissionOKHasReadPermission() {
        User testUser = this.createTestUser();
        testUser.setPermissions(this.dataPoint.getReadPermission());
        Permissions.ensureDataPointReadPermission(testUser, this.dataPoint);
    }

    @Test
    public void ensureDataPointSetPermissionOKHasDataSourcePermission() {
        User testUser = this.createTestUser();
        testUser.setPermissions(this.dataSource.getEditPermission());
        Permissions.ensureDataPointSetPermission(testUser, this.dataPoint);
    }

    @Test
    public void ensureDataPointSetPermissionOKHasSetPermission() {
        User testUser = this.createTestUser();
        testUser.setPermissions(this.dataPoint.getSetPermission());
        Permissions.ensureDataPointSetPermission(testUser, this.dataPoint);
    }

    @Test(expected = PermissionException.class)
    public void ensureDataPointSetPermissionFailHasReadPermission() {
        User testUser = this.createTestUser();
        testUser.setPermissions(this.dataPoint.getReadPermission());
        Permissions.ensureDataPointSetPermission(testUser, this.dataPoint);
    }

    @Test
    public void explodePermissionGroupsNullSize0() {
        assertEquals(0, Permissions.explodePermissionGroups(null).size());
    }

    @Test
    public void explodePermissionGroupsEmptySize0() {
        assertEquals(0, Permissions.explodePermissionGroups("").size());
    }

    @Test
    public void explodePermissionGroupsSingleSize1() {
        assertEquals(1, Permissions.explodePermissionGroups(this.randomPermission()).size());
    }

    @Test
    public void explodePermissionGroupsTwoSize2() {
        assertEquals(2, Permissions.explodePermissionGroups(this.randomPermission() + "," + this.randomPermission()).size());
    }

    @Test
    public void explodePermissionGroupsNoDups() {
        String perm = this.randomPermission();
        assertEquals(1, Permissions.explodePermissionGroups(perm + "," + perm).size());
    }

    @Test
    public void explodePermissionGroupsRemoveEmpty() {
        assertEquals(1, Permissions.explodePermissionGroups(this.randomPermission() + ",").size());
    }

    @Test
    public void explodePermissionGroupsTrim() {
        String perm = this.randomPermission();
        assertEquals(1, Permissions.explodePermissionGroups(perm + ", " + perm + " ").size());
    }

    @Test
    public void implodePermissionGroups() {
        String perm1 = this.randomPermission();
        String perm2 = this.randomPermission();

        Set<String> permSet = new HashSet<>();
        permSet.add(perm1);
        permSet.add(perm2);

        String joinedPerms = Permissions.implodePermissionGroups(permSet);

        assertTrue(joinedPerms.contains(perm1));
        assertTrue(joinedPerms.contains(perm2));
        assertTrue(joinedPerms.contains(","));
        assertFalse(joinedPerms.contains(" "));

        assertTrue(permSet.equals(Permissions.explodePermissionGroups(joinedPerms)));
    }

    @Test
    public void ensureHasSinglePermissionOKSuperadmin() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasSinglePermission(testUser, this.randomPermission());
    }

    @Test
    public void ensureHasSinglePermissionOKSuperadminNullPerm() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasSinglePermission(testUser, null);
    }

    @Test
    public void ensureHasSinglePermissionOKSuperadminEmptyPerm() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasSinglePermission(testUser, "");
    }

    @Test
    public void ensureHasSinglePermissionOKHasPerm() {
        String perm1 = this.randomPermission();
        String perm2 = this.randomPermission();

        User testUser = this.createTestUser();
        testUser.setPermissions(perm1 + "," + perm2);

        Permissions.ensureHasSinglePermission(testUser, perm1);
        Permissions.ensureHasSinglePermission(testUser, perm2);
    }

    @Test(expected = PermissionException.class)
    public void ensureHasSinglePermissionFailNullPerm() {
        User testUser = this.createTestUser();
        Permissions.ensureHasSinglePermission(testUser, null);
    }

    @Test(expected = PermissionException.class)
    public void ensureHasSinglePermissionFailEmptyPerm() {
        User testUser = this.createTestUser();
        Permissions.ensureHasSinglePermission(testUser, "");
    }

    @Test
    public void ensureHasAnyPermissionOKSuperadmin() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAnyPermission(testUser, Sets.newHashSet(this.randomPermission(), this.randomPermission()));
    }

    @Test
    public void ensureHasAnyPermissionOKSuperadminEmptySet() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAnyPermission(testUser, Collections.emptySet());
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAnyPermissionFailEmptySet() {
        User testUser = this.createTestUser();
        Permissions.ensureHasAnyPermission(testUser, Collections.emptySet());
    }

    @Test
    public void ensureHasAnyPermissionOKHasOne() {
        String perm1 = this.randomPermission();
        String perm2 = this.randomPermission();

        User testUser = this.createTestUser();
        testUser.setPermissions(perm1 + "," + this.randomPermission());

        Permissions.ensureHasAnyPermission(testUser, Sets.newHashSet(perm1, perm2));
    }

    @Test
    public void ensureHasAnyPermissionOKHasBoth() {
        String perm1 = this.randomPermission();
        String perm2 = this.randomPermission();

        User testUser = this.createTestUser();
        testUser.setPermissions(perm1 + "," + perm2);

        Permissions.ensureHasAnyPermission(testUser, Sets.newHashSet(perm1, perm2));
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAnyPermissionFailHasNeither() {
        String perm1 = this.randomPermission();
        String perm2 = this.randomPermission();

        User testUser = this.createTestUser();
        testUser.setPermissions(this.randomPermission() + "," + this.randomPermission());

        Permissions.ensureHasAnyPermission(testUser, Sets.newHashSet(perm1, perm2));
    }

    @Test(expected = NullPointerException.class)
    public void ensureHasAnyPermissionFailNullSet() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAnyPermission(testUser, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureHasAnyPermissionFailEmptyEntry() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAnyPermission(testUser, Collections.singleton(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureHasAnyPermissionFailNullEntry() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAnyPermission(testUser, Collections.singleton(null));
    }

    @Test
    public void ensureHasAllPermissionsOKSuperadmin() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAllPermissions(testUser, Sets.newHashSet(this.randomPermission(), this.randomPermission()));
    }

    @Test
    public void ensureHasAllPermissionsOKSuperadminEmptySet() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAllPermissions(testUser, Collections.emptySet());
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAllPermissionsFailEmptySet() {
        User testUser = this.createTestUser();
        Permissions.ensureHasAllPermissions(testUser, Collections.emptySet());
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAllPermissionsFailHasOne() {
        String perm1 = this.randomPermission();
        String perm2 = this.randomPermission();

        User testUser = this.createTestUser();
        testUser.setPermissions(perm1 + "," + this.randomPermission());

        Permissions.ensureHasAllPermissions(testUser, Sets.newHashSet(perm1, perm2));
    }

    @Test
    public void ensureHasAllPermissionsOKHasBoth() {
        String perm1 = this.randomPermission();
        String perm2 = this.randomPermission();

        User testUser = this.createTestUser();
        testUser.setPermissions(perm1 + "," + perm2);

        Permissions.ensureHasAllPermissions(testUser, Sets.newHashSet(perm1, perm2));
    }

    @Test(expected = PermissionException.class)
    public void ensureHasAllPermissionsFailHasNeither() {
        String perm1 = this.randomPermission();
        String perm2 = this.randomPermission();

        User testUser = this.createTestUser();
        testUser.setPermissions(this.randomPermission() + "," + this.randomPermission());

        Permissions.ensureHasAllPermissions(testUser, Sets.newHashSet(perm1, perm2));
    }

    @Test(expected = NullPointerException.class)
    public void ensureHasAllPermissionsFailNullSet() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAllPermissions(testUser, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureHasAllPermissionsFailEmptyEntry() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAllPermissions(testUser, Collections.singleton(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureHasAllPermissionsFailNullEntry() {
        User testUser = this.createTestUser();
        testUser.setPermissions(SUPERADMIN);
        Permissions.ensureHasAllPermissions(testUser, Collections.singleton(null));
    }
    
    @Test
    public void ensureCannotRemoveAccess() {
        User testUser = this.createTestUser();
        DataPointVO dp = this.createDataPoint(this.dataSource, testUser.getPermissions(), testUser.getPermissions());
        BackgroundContext.set(testUser);
        dp.setSetPermission("");
        try{
            dp.ensureValid();
            fail("Should be invalid");
        }catch(ValidationException e) {
            assertTrue(e.getValidationResult().getHasMessages());
            assertEquals(e.getValidationResult().getMessages().get(0).getContextKey(), "setPermission");
        }
    }
    
    @Test
    public void ensureCannotAddNewRole() {
        User testUser = this.createTestUser();
        DataPointVO dp = this.createDataPoint(this.dataSource, testUser.getPermissions(), testUser.getPermissions());
        BackgroundContext.set(testUser);
        dp.setSetPermission("new_role");
        try{
            dp.ensureValid();
            fail("Should be invalid");
        }catch(ValidationException e) {
            assertTrue(e.getValidationResult().getHasMessages());
            assertEquals(e.getValidationResult().getMessages().get(0).getContextKey(), "setPermission");
        }
    }
}
