/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooq.Condition;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.tables.Events;
import com.infiniteautomation.mango.db.tables.records.EventsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EventInstanceVO;

/**
 * TODO: This test will cause NPE due to the fact that audit events are being raised and there is no audit event type for
 *  EventInstanceVOs
 * @author Terry Packer
 */
public class EventInstanceServiceTest extends AbstractVOServiceWithPermissionsTest<EventInstanceVO, EventsRecord, Events, EventInstanceDao, EventInstanceService> {

    @Override
    String getCreatePermissionType() {
        return null;
    }

    @Override
    void setReadPermission(MangoPermission permission, EventInstanceVO vo) {
        vo.setReadPermission(permission);
    }

    @Override
    String getReadPermissionContextKey() {
        return "readPermission";
    }

    @Override
    void setEditPermission(MangoPermission permission, EventInstanceVO vo) {

    }

    @Override
    String getEditPermissionContextKey() {
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

    @Override
    @Test
    public void testCountQueryReadPermissionEnforcement() {
    }

    @Override
    @Test
    public void testCountQueryEditPermissionEnforcement() {

    }

    @Override
    @Test
    public void testEditRolesCannotBeNull() {

    }

    //Overrident tests
    @Override
    @Test
    public void testCreatePrivilegeSuccess() {
        EventInstanceVO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(roleService.getUserRole()), vo);
        // TODO is this runAs needed?
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            service.insert(vo);
        });
    }

    @Override
    @Test
    public void testQueryReadPermissionEnforcement() {
        EventInstanceVO vo = newVO(editUser);
        setReadPermission(MangoPermission.requireAnyRole(editRole), vo);
        EventInstanceVO saved = service.insert(vo);
        runAs.runAs(readUser, () -> {
            ConditionSortLimit conditions = new ConditionSortLimit(null, null, 1, 0);
            AtomicInteger count = new AtomicInteger();
            getService().customizedQuery(conditions, (item) -> {
                count.getAndIncrement();
            });
            assertEquals(0, count.get());
        });
        runAs.runAs(editUser, () -> {
            Condition c = getDao().getIdField().eq(saved.getId());
            ConditionSortLimit conditions = new ConditionSortLimit(c, null, null, null);
            AtomicInteger count = new AtomicInteger();
            getService().customizedQuery(conditions, (item) -> {
                count.getAndIncrement();
                assertEquals(saved.getId(), item.getId());
            });
            assertEquals(1, count.get());
        });
    }
}
