/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionException;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerServiceTest extends AbstractVOServiceTest<SetPointEventHandlerVO, EventHandlerDao<SetPointEventHandlerVO>, EventHandlerService<SetPointEventHandlerVO>> {
    
    private DataPointVO activeDataPoint;
    private DataPointVO inactiveDataPoint;
    private DataPointService dataPointService;
    protected DataSourceService<MockDataSourceVO> dataSourceService;
    
    @SuppressWarnings("unchecked")
    @Before
    public void beforeSetPointEventHandlerServiceTest() {
        dataSourceService = Common.getBean(DataSourceService.class);
        dataPointService = Common.getBean(DataPointService.class);  
        MockDataSourceVO dsVo = createDataSource(Collections.emptySet());
        activeDataPoint = createDataPoint(dsVo, Collections.singleton(readRole), Collections.singleton(readRole));
        inactiveDataPoint = createDataPoint(dsVo, Collections.singleton(readRole), Collections.singleton(readRole));
    }
    
    MockDataSourceVO createDataSource(Set<RoleVO> editRoles) {
        MockDataSourceVO dsVo = new MockDataSourceVO();
        dsVo.setName("permissions_test_datasource");
        dsVo.setEditRoles(editRoles);
        return dataSourceService.insertFull(dsVo, systemSuperadmin);
    }
    
    DataPointVO createDataPoint(DataSourceVO<?> dsVo, Set<RoleVO> readRoles, Set<RoleVO> setRoles) {
        DataPointVO point = new DataPointVO();
        point.setDataSourceId(dsVo.getId());
        point.setName("permissions_test_datasource");
        point.setReadRoles(readRoles);
        point.setSetRoles(setRoles);
        point.setPointLocator(new MockPointLocatorVO(DataTypes.NUMERIC, true));
        dataPointService.insertFull(point, systemSuperadmin);
        return point;
    }
    
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
        SetPointEventHandlerVO vo = newVO();
        service.insertFull(vo, editUser);
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            SetPointEventHandlerVO vo = newVO();
            addRoleToCreatePermission(editRole);
            service.insertFull(vo, editUser);
        });
    }
    
    @SuppressWarnings("unchecked")
    @Override
    EventHandlerService<SetPointEventHandlerVO> getService() {
        return Common.getBean(EventHandlerService.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
    EventHandlerDao getDao() {
        return EventHandlerDao.getInstance();
    }

    @Override
    void assertVoEqual(SetPointEventHandlerVO expected, SetPointEventHandlerVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
        assertRoles(expected.getScriptRoles().getRoles(), actual.getScriptRoles().getRoles());

        //TODO assert remaining
    }

    @Override
    SetPointEventHandlerVO newVO() {
        SetPointEventHandlerVO vo = (SetPointEventHandlerVO) ModuleRegistry.getEventHandlerDefinition(SetPointEventHandlerDefinition.TYPE_NAME).baseCreateEventHandlerVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        ScriptPermissions permissions = new ScriptPermissions(Collections.singleton(readRole));
        vo.setScriptRoles(permissions);
        vo.setActiveScript("return true;");
        vo.setInactiveScript("return true;");
        vo.setTargetPointId(activeDataPoint.getId());
        vo.setActiveAction(SetPointEventHandlerVO.SET_ACTION_POINT_VALUE);
        vo.setInactivePointId(inactiveDataPoint.getId());
        vo.setInactiveAction(SetPointEventHandlerVO.SET_ACTION_POINT_VALUE);
        return vo;
    }

    @Override
    SetPointEventHandlerVO updateVO(SetPointEventHandlerVO existing) {
        return existing;
    }
    
    void addRoleToCreatePermission(RoleVO vo) {
        roleService.addRoleToPermission(vo, EventHandlerCreatePermission.PERMISSION, systemSuperadmin);
    }

}
