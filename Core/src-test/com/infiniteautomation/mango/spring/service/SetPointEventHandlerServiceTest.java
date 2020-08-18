/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.EventHandlerTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockRuntimeManager;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerServiceTest extends AbstractVOServiceTest<AbstractEventHandlerVO, EventHandlerTableDefinition, EventHandlerDao, EventHandlerService> {

    private DataPointVO activeDataPoint;
    private DataPointVO inactiveDataPoint;
    private DataPointService dataPointService;
    protected DataSourceService dataSourceService;

    public SetPointEventHandlerServiceTest() {
    }

    @Before
    public void beforeSetPointEventHandlerServiceTest() {
        dataSourceService = Common.getBean(DataSourceService.class);
        dataPointService = Common.getBean(DataPointService.class);
        MockDataSourceVO dsVo = createDataSource(Collections.emptySet());
        activeDataPoint = createDataPoint(dsVo, Collections.singleton(readRole), Collections.singleton(readRole));
        inactiveDataPoint = createDataPoint(dsVo, Collections.singleton(readRole), Collections.singleton(readRole));
    }

    MockDataSourceVO createDataSource(Set<Role> editRoles) {
        MockDataSourceVO dsVo = new MockDataSourceVO();
        dsVo.setName("permissions_test_datasource");
        dsVo.setEditPermission(MangoPermission.requireAnyRole(editRoles));
        return (MockDataSourceVO) getService().permissionService.runAsSystemAdmin(() -> {
            return dataSourceService.insert(dsVo);
        });
    }

    DataPointVO createDataPoint(DataSourceVO dsVo, Set<Role> readRoles, Set<Role> setRoles) {
        return getService().permissionService.runAsSystemAdmin(() -> {
            DataPointVO point = new DataPointVO();
            point.setDataSourceId(dsVo.getId());
            point.setName("permissions_test_datasource");
            point.setReadPermission(MangoPermission.requireAnyRole(readRoles));
            point.setSetPermission(MangoPermission.requireAnyRole(setRoles));
            point.setPointLocator(new MockPointLocatorVO(DataTypes.NUMERIC, true));

            point = dataPointService.insert(point);
            return point;
        });
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setRuntimeManager(new MockRuntimeManager(true));
        return lifecycle;
    }

    @Test(expected = PermissionException.class)
    public void testCreatePrivilegeFails() {
        SetPointEventHandlerVO vo = newVO(editUser);
        getService().permissionService.runAs(editUser, () -> {
            service.insert(vo);
        });
    }

    @Test
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            SetPointEventHandlerVO vo = newVO(editUser);
            vo.setReadPermission(MangoPermission.requireAnyRole(editRole));
            vo.setEditPermission(MangoPermission.requireAnyRole(editRole));
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
            SetPointEventHandlerVO vo = newVO(readUser);
            ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
            vo.setScriptRoles(permissions);
            getService().permissionService.runAsSystemAdmin(() -> {
                service.insert(vo);
                SetPointEventHandlerVO fromDb = (SetPointEventHandlerVO) service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                roleService.delete(editRole.getId());
                roleService.delete(readRole.getId());
                SetPointEventHandlerVO updated = (SetPointEventHandlerVO) service.get(fromDb.getId());
                fromDb.setScriptRoles(new ScriptPermissions(Collections.emptySet()));
                assertVoEqual(fromDb, updated);
            });
        });
    }

    @Test(expected = NotFoundException.class)
    @Override
    public void testDelete() {
        runTest(() -> {
            SetPointEventHandlerVO vo = newVO(readUser);
            ScriptPermissions permissions = new ScriptPermissions(Sets.newHashSet(readRole, editRole));
            vo.setScriptRoles(permissions);
            getService().permissionService.runAsSystemAdmin(() -> {
                service.update(vo.getXid(), vo);
                SetPointEventHandlerVO fromDb = (SetPointEventHandlerVO) service.get(vo.getId());
                assertVoEqual(vo, fromDb);
                service.delete(vo.getId());
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
        assertRoles(((SetPointEventHandlerVO)expected).getScriptRoles().getRoles(), ((SetPointEventHandlerVO)actual).getScriptRoles().getRoles());

        //TODO assert remaining
    }

    @Override
    SetPointEventHandlerVO newVO(User owner) {
        SetPointEventHandlerVO vo = (SetPointEventHandlerVO) ModuleRegistry.getEventHandlerDefinition(SetPointEventHandlerDefinition.TYPE_NAME).baseCreateEventHandlerVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setName(UUID.randomUUID().toString());
        ScriptPermissions permissions = new ScriptPermissions(Collections.singleton(readRole));
        vo.setScriptRoles(permissions);
        vo.setActiveScript("return true;");
        vo.setInactiveScript("return true;");
        vo.setTargetPointId(activeDataPoint.getId());
        vo.setActivePointId(activeDataPoint.getId());
        vo.setActiveAction(SetPointEventHandlerVO.SET_ACTION_POINT_VALUE);
        vo.setInactivePointId(inactiveDataPoint.getId());
        vo.setInactiveAction(SetPointEventHandlerVO.SET_ACTION_POINT_VALUE);
        return vo;
    }

    @Override
    AbstractEventHandlerVO updateVO(AbstractEventHandlerVO existing) {
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
            Common.getBean(SystemPermissionService.class).update(new MangoPermission(newRoles), def);
        });
    }

}
