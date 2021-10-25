/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.MockEventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.event.EventInstanceVO;

import static org.junit.Assert.assertEquals;

public class EventInstanceDaoTest extends AbstractVoDaoTest<EventInstanceVO, EventInstanceDao> {

    UserCommentDao userCommentDao;
    EventDao eventDao;

    @Override
    public void before() {
        super.before();
        userCommentDao = Common.getBean(UserCommentDao.class);
        eventDao = Common.getBean(EventDao.class);
    }

    @Test
    public void testPurgeEvents() {
        long refTime = System.currentTimeMillis();
        createEvents(10, 2, refTime);
        assertEquals(10, eventDao.getEventCount());
        assertEquals(20, userCommentDao.count());

        int count = eventDao.purgeAllEvents();
        assertEquals(10, count);

        assertEquals(0, eventDao.getEventCount());
        assertEquals(0, userCommentDao.count());
    }

    @Test
    public void testPurgeEventsBefore() {
        long refTime = System.currentTimeMillis();
        createEvents(10, 2, refTime);
        assertEquals(10, eventDao.getEventCount());
        assertEquals(20, userCommentDao.count());

        int count = eventDao.purgeEventsBefore(refTime);
        assertEquals(5, count);

        assertEquals(5, eventDao.getEventCount());
        assertEquals(10, userCommentDao.count());
    }

    @Test
    public void testPurgeEventsBeforeByType() {
        long refTime = System.currentTimeMillis();
        createEvents(10, 2, refTime);
        assertEquals(10, eventDao.getEventCount());
        assertEquals(20, userCommentDao.count());

        String eventType = newVO().getEventType().getEventType();

        int count = eventDao.purgeEventsBefore(refTime, eventType);
        assertEquals(5, count);

        assertEquals(5, eventDao.getEventCount());
        assertEquals(10, userCommentDao.count());
    }

    @Test
    public void testPurgeEventsBeforeByAlarmLevel() {
        long refTime = System.currentTimeMillis();
        createEvents(10, 2, refTime);
        assertEquals(10, eventDao.getEventCount());
        assertEquals(20, userCommentDao.count());

        AlarmLevels alarmLevel = newVO().getAlarmLevel();
        int count = eventDao.purgeEventsBefore(refTime, alarmLevel);
        assertEquals(5, count);
        assertEquals(5, eventDao.getEventCount());
        assertEquals(10, userCommentDao.count());
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
    EventInstanceVO newVO() {
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
            userCommentDao.insert(comment);
            comments.add(comment);
        }
        return comments;
    }

    void createEvents(int eventCount, int commentCount, long refTime) {
        long beforeRef = refTime - 1000;
        long afterRef = refTime + 1000;
        for (int i = 0; i < eventCount; i++) {
            EventInstanceVO vo = newVO();
            vo.setActiveTimestamp(i % 2 == 0 ? beforeRef : afterRef);
            dao.insert(vo);
            createComments(vo.getId(), commentCount, refTime);
        }
    }
}
