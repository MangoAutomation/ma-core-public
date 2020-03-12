/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.UUID;

import com.infiniteautomation.mango.spring.db.DataSourceTableDefinition;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.module.definitions.permissions.DataSourcePermissionDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class DataSourceServiceTest extends AbstractVOServiceWithPermissionsTest<DataSourceVO, DataSourceTableDefinition, DataSourceDao, DataSourceService> {

    @Override
    String getCreatePermissionType() {
        return DataSourcePermissionDefinition.PERMISSION;
    }

    @Override
    void setReadRoles(Set<Role> roles, DataSourceVO vo) {
        vo.setReadRoles(roles);
    }

    @Override
    void setEditRoles(Set<Role> roles, DataSourceVO vo) {
        vo.setEditRoles(roles);
    }

    @Override
    DataSourceService getService() {
        return Common.getBean(DataSourceService.class);
    }

    @Override
    DataSourceDao getDao() {
        return DataSourceDao.getInstance();
    }

    @Override
    void assertVoEqual(DataSourceVO expected, DataSourceVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDefinition().getDataSourceTypeName(), actual.getDefinition().getDataSourceTypeName());

        assertRoles(expected.getEditRoles(), actual.getEditRoles());
        //TODO Flesh out all fields
    }

    @Override
    DataSourceVO newVO(User user) {
        MockDataSourceVO vo = new MockDataSourceVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        //TODO Flesh out all fields
        return vo;
    }

    @Override
    DataSourceVO updateVO(DataSourceVO existing) {
        DataSourceVO copy = (DataSourceVO) existing.copy();
        copy.setName("new name");

        return copy;
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setRuntimeManager(new MockRuntimeManager(true));
        return lifecycle;
    }

    @Override
    void addReadRoleToFail(Role role, DataSourceVO vo) {
        vo.getReadRoles().add(role);
    }

    @Override
    String getReadRolesContextKey() {
        return "readRoles";
    }

    @Override
    void addEditRoleToFail(Role role, DataSourceVO vo) {
        vo.getEditRoles().add(role);
    }

    @Override
    String getEditRolesContextKey() {
        return "editRoles";
    }

}
