/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.jooq.Condition;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.tables.Events;
import com.infiniteautomation.mango.db.tables.records.EventsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.rt.event.UserEventLevelSummary;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.event.EventInstanceVO;

/**
 * TODO: This test will cause NPE due to the fact that audit events are being raised and there is no audit event type for
 *  EventInstanceVOs
 * @author Terry Packer
 */
public class EventInstanceServiceTest extends AbstractVOServiceWithPermissionsTest<EventInstanceVO, EventsRecord, Events, EventInstanceDao, EventInstanceService> {

    UserCommentService userCommentService;
    EventDao eventDao;

    @Override
    public void before() {
        super.before();
        userCommentService = Common.getBean(UserCommentService.class);
        eventDao = Common.getBean(EventDao.class);
    }

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
        assertEquals(expected.getActiveTimestamp(), actual.getActiveTimestamp());
        assertEquals(expected.isRtnApplicable(), actual.isRtnApplicable());
        assertEquals(expected.getMessage(), actual.getMessage());

        List<UserCommentVO> actualComments = actual.getEventComments();
        List<UserCommentVO> expectedComments = expected.getEventComments();
        assertEquals(expectedComments.size(), actualComments.size());
        for (int i = 0; i < expectedComments.size(); i++) {
            assertEquals(expectedComments.get(i).getId(), actualComments.get(i).getId());
        }
    }

    @Override
    EventInstanceVO newVO(User owner) {
        EventInstanceVO vo = new EventInstanceVO();
        vo.setAlarmLevel(AlarmLevels.URGENT);
        vo.setEventType(new MockEventType(readRole));
        vo.setActiveTimestamp(System.currentTimeMillis());
        vo.setRtnApplicable(false);
        vo.setMessage(new TranslatableMessage("common.noMessage"));
        return vo;
    }

    @Override
    EventInstanceVO updateVO(EventInstanceVO existing) {
        EventInstanceVO copy = (EventInstanceVO) existing.copy();
        copy.setAlarmLevel(AlarmLevels.CRITICAL);
        copy.setActiveTimestamp(System.currentTimeMillis());
        copy.setRtnApplicable(true);
        copy.setMessage(new TranslatableMessage("common.noMessage"));
        copy.setEventComments(createComments(copy.getId(), 10, System.currentTimeMillis()));
        return copy;
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

    @Test
    public void testPurgeEvents() {
        long refTime = System.currentTimeMillis();
        createEvents(10, 2, refTime);
        assertEquals(10, eventDao.getEventCount());
        assertEquals(20, userCommentService.dao.count());

        runAs.runAs(runAs.systemSuperadmin(), () -> {
            int count = eventDao.purgeAllEvents();
            assertEquals(10, count);
        });
        assertEquals(0, eventDao.getEventCount());
        assertEquals(0, userCommentService.dao.count());
    }

    @Test
    public void testPurgeEventsBefore() {
        long refTime = System.currentTimeMillis();
        createEvents(10, 2, refTime);
        assertEquals(10, eventDao.getEventCount());
        assertEquals(20, userCommentService.dao.count());

        runAs.runAs(runAs.systemSuperadmin(), () -> {
            int count = eventDao.purgeEventsBefore(refTime);
            assertEquals(5, count);
        });
        assertEquals(5, eventDao.getEventCount());
        assertEquals(10, userCommentService.dao.count());
    }

    @Test
    public void testPurgeEventsBeforeByType() {
        long refTime = System.currentTimeMillis();
        createEvents(10, 2, refTime);
        assertEquals(10, eventDao.getEventCount());
        assertEquals(20, userCommentService.dao.count());

        String eventType = newVO(editUser).getEventType().getEventType();
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            int count = eventDao.purgeEventsBefore(refTime, eventType);
            assertEquals(5, count);
        });
        assertEquals(5, eventDao.getEventCount());
        assertEquals(10, userCommentService.dao.count());
    }

    @Test
    public void testPurgeEventsBeforeByAlarmLevel() {
        long refTime = System.currentTimeMillis();
        createEvents(10, 2, refTime);
        assertEquals(10, eventDao.getEventCount());
        assertEquals(20, userCommentService.dao.count());

        AlarmLevels alarmLevel = newVO(editUser).getAlarmLevel();
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            int count = eventDao.purgeEventsBefore(refTime, alarmLevel);
            assertEquals(5, count);
        });
        assertEquals(5, eventDao.getEventCount());
        assertEquals(10, userCommentService.dao.count());
    }

    @Test
    public void testUnackSummaryCount() {
        long refTime = System.currentTimeMillis();
        createEvents(5, 0, refTime);
        List<UserEventLevelSummary> summaries = service.getUnacknowledgedSummary();
        AlarmLevels alarmLevel = newVO(editUser).getAlarmLevel();
        UserEventLevelSummary summary = summaries.get(alarmLevel.value());
        assertEquals(5,  summary.getCount());
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

    void createEvents(int eventCount, int commentCount, long refTime) {
        long beforeRef = refTime - 1000;
        long afterRef = refTime + 1000;
        for (int i = 0; i < eventCount; i++) {
            EventInstanceVO vo = newVO(editUser);
            vo.setActiveTimestamp(i % 2 == 0 ? beforeRef : afterRef);
            EventInstanceVO inserted = service.insert(vo);
            createComments(inserted.getId(), commentCount, refTime);
        }
    }

    List<UserCommentVO> createComments(int eventId, int count, long time) {
        List<UserCommentVO> comments = new ArrayList<>();
        for(int i = 0; i < count; i++) {
            UserCommentVO comment = new UserCommentVO();
            comment.setXid(UUID.randomUUID().toString());
            comment.setUserId(readUser.getId());
            comment.setTs(time);
            comment.setComment(UUID.randomUUID().toString());
            comment.setCommentType(UserCommentVO.TYPE_EVENT);
            comment.setReferenceId(eventId);
            comment.setUsername(readUser.getUsername());
            comments.add(userCommentService.insert(comment));
        }
        return comments;
    }
}
