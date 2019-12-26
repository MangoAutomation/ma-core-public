/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.UUID;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;

/**
 * @author Terry Packer
 *
 */
public class DataSourceServiceTest<T extends DataSourceVO<T>> extends ServiceWithPermissionsTestBase<T, DataSourceDao<T>, DataSourceService<T>> {

    @Override
    String getCreatePermissionType() {
        return SystemSettingsDao.PERMISSION_DATASOURCE;
    }

    @Override
    void setReadRoles(Set<RoleVO> roles, T vo) {
        vo.setEditRoles(roles);
    }

    @Override
    void setEditRoles(Set<RoleVO> roles, T vo) {
        vo.setEditRoles(roles);
    }

    @SuppressWarnings("unchecked")
    @Override
    DataSourceService<T> getService() {
        return Common.getBean(DataSourceService.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    DataSourceDao<T> getDao() {
        return (DataSourceDao<T>) DataSourceDao.getInstance();
    }

    @Override
    void assertVoEqual(T expected, T actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDefinition().getDataSourceTypeName(), actual.getDefinition().getDataSourceTypeName());
        
        assertRoles(expected.getEditRoles(), actual.getEditRoles());
        //TODO Flesh out all fields
    }

    @SuppressWarnings("unchecked")
    @Override
    T newVO() {
        MockDataSourceVO vo = new MockDataSourceVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        //TODO Flesh out all fields
        return (T) vo;
    }

    @Override
    T updateVO(T existing) {
        T copy = existing.copy();
        copy.setName("new name");
        
        return copy;
    }
    
    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setRuntimeManager(new MockRuntimeManager(true));
        return lifecycle;
    }

}
