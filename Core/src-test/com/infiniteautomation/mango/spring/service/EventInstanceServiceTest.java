/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.jooq.Condition;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.EventInstanceTableDefinition;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.vo.role.Role;

/**
 *
 * @author Terry Packer
 */
public class EventInstanceServiceTest extends AbstractVOServiceWithPermissionsTest<EventInstanceVO, EventInstanceTableDefinition, EventInstanceDao, EventInstanceService> {

    @Override
    String getCreatePermissionType() {
        return null;
    }

    @Override
    void setReadPermission(MangoPermission permission, EventInstanceVO vo) {
        vo.setReadPermission(permission);
    }

    @Override
    void addReadRoleToFail(Role role, EventInstanceVO vo) {
        vo.getReadPermission().getRoles().add(Collections.singleton(role));
    }

    @Override
    String getReadRolesContextKey() {
        return "readPermission";
    }

    @Override
    void setEditPermission(MangoPermission permission, EventInstanceVO vo) {

    }

    @Override
    void addEditRoleToFail(Role role, EventInstanceVO vo) {

    }

    @Override
    String getEditRolesContextKey() {
        return null;
    }

    @Override
    EventInstanceService getService() {
        return Common.getBean(EventInstanceService.class);
    }

    @Override
    EventInstanceDao getDao() {
        return Common.getBean(EventInstanceDao.class);
    }

    @Override
    void assertVoEqual(EventInstanceVO expected, EventInstanceVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getAlarmLevel(), actual.getAlarmLevel());
    }

    @Override
    EventInstanceVO newVO(User owner) {
        EventInstanceVO vo = new EventInstanceVO();
        vo.setName("test");
        vo.setXid("xid");
        vo.setAlarmLevel(AlarmLevels.URGENT);
        vo.setEventType(new MockEventType(readRole));
        return vo;
    }

    @Override
    EventInstanceVO updateVO(EventInstanceVO existing) {
        existing.setAlarmLevel(AlarmLevels.CRITICAL);
        return existing;
    }

    //Skipped Tests
    @Override
    @Test
    public void testUserCanDelete() {

    }

    @Override
    @Test
    public void testCannotRemoveEditAccess() {

    }

    @Override
    @Test
    public void testAddReadRoleUserDoesNotHave() {
        //No edit permission
    }

    @Override
    @Test
    public void testCannotModifySetRoles() {
        //No edit permission
    }

    @Override
    @Test
    public void testQueryEditPermissionEnforcement() {
        //No edit permission
    }

    @Override
    @Test
    public void testCannotRemoveReadAccess() {
        //No edit permission
    }

    @Override
    @Test
    public void testUserEditRole() {
        //No edit permission
    }

    @Override
    @Test
    public void testAddEditRoleUserDoesNotHave() {
        //No edit permission
    }

    @Override
    @Test
    public void testUpdateViaXid() {
        //No xid
    }

    @Override
    @Test
    public void testDeleteViaXid() {
        //No xid
    }

    //Overrident tests
    @Override
    @Test
    public void testCreatePrivilegeSuccess() {
        runTest(() -> {
            EventInstanceVO vo = newVO(editUser);
            setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
            getService().permissionService.runAs(systemSuperadmin, () -> {
                service.insert(vo);
            });
        });
    }

    @Override
    @Test
    public void testQueryReadPermissionEnforcement() {
        EventInstanceVO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getSuperadminRole(), editRole), vo);
        EventInstanceVO saved = getService().permissionService.runAsSystemAdmin(() -> {
            return service.insert(vo);
        });
        getService().permissionService.runAs(readUser, () -> {
            ConditionSortLimit conditions = new ConditionSortLimit(null, null, 1, 0);
            AtomicInteger count = new AtomicInteger();
            getService().customizedQuery(conditions, (item, row) -> {
                count.getAndIncrement();
            });
            assertEquals(0, count.get());
        });
        getService().permissionService.runAs(editUser, () -> {
            Condition c = getDao().getTable().getIdAlias().eq(saved.getId());
            ConditionSortLimit conditions = new ConditionSortLimit(c, null, null, null);
            AtomicInteger count = new AtomicInteger();
            getService().customizedQuery(conditions, (item, row) -> {
                count.getAndIncrement();
                assertEquals(saved.getId(), item.getId());
            });
            assertEquals(1, count.get());
        });
    }
}
