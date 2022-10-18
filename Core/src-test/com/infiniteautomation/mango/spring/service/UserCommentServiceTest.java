package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.db.tables.UserComments;
import com.infiniteautomation.mango.db.tables.records.UserCommentsRecord;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockEventManager;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.UserCommentDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.comment.UserCommentVO;

/**
 * @author Mert Cingöz
 */
public class UserCommentServiceTest extends AbstractVOServiceTest<UserCommentVO, UserCommentsRecord, UserComments, UserCommentDao, UserCommentService> {

    private IDataPoint dataPoint;

    @Before
    public void setup (){
        dataPoint = createMockDataPoints(1).get(0);
    }

    @Override
    UserCommentService getService() {
        return Common.getBean(UserCommentService.class);
    }

    @Override
    UserCommentDao getDao() {
        return UserCommentDao.getInstance();
    }

    @Override
    void assertVoEqual(UserCommentVO expected, UserCommentVO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getXid(), actual.getXid());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getComment(), actual.getComment());
        assertEquals(expected.getUserId(), actual.getUserId());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getReferenceId(), actual.getReferenceId());
        assertEquals(expected.getTs(), actual.getTs());
        assertEquals(expected.getCommentType(), actual.getCommentType());
    }

    @Override
    UserCommentVO newVO(User owner) {
        UserCommentVO vo = new UserCommentVO();
        vo.setXid(UUID.randomUUID().toString());
        vo.setUserId(readUser.getId());
        vo.setTs(Common.timer.currentTimeMillis());
        vo.setComment(UUID.randomUUID().toString());
        vo.setCommentType(UserCommentVO.TYPE_POINT);
        vo.setReferenceId(dataPoint.getId());
        vo.setUsername(readUser.getUsername());
        return vo;
    }

    @Override
    UserCommentVO updateVO(UserCommentVO existing) {
        UserCommentVO copy = (UserCommentVO) existing.copy();
        copy.setTs(System.currentTimeMillis());
        copy.setComment(UUID.randomUUID().toString());
        return copy;
    }

    @Test
    public void testManageEventComments() {
        SystemEventType type = new SystemEventType(SystemEventType.TYPE_SYSTEM_STARTUP);
        long timestamp = Common.timer.currentTimeMillis();
        Common.eventManager.raiseEvent(type, timestamp, true, AlarmLevels.CRITICAL,
                new TranslatableMessage("common.default", "testing"), null);

        EventInstance activeEvent = Common.eventManager.getById(1);
        List<UserCommentVO> comments = activeEvent.getEventComments();
        Assert.assertEquals(0, comments.size());

        // insert
        UserCommentVO newComment = newVO(readUser);
        newComment.setCommentType(UserCommentVO.TYPE_EVENT);
        newComment.setReferenceId(activeEvent.getId());

        service.insert(newComment);
        comments = activeEvent.getEventComments();

        Assert.assertEquals(1, comments.size());
        UserCommentVO activeEventComment = comments.get(0);
        assertVoEqual(newComment, activeEventComment);

        // update
        UserCommentVO updatedComment = updateVO(newComment);
        service.update(newComment.getId(), updatedComment);
        comments = activeEvent.getEventComments();

        Assert.assertEquals(1, comments.size());
        activeEventComment = comments.get(0);
        assertVoEqual(updatedComment, activeEventComment);

        // delete
        service.delete(updatedComment.getId());
        comments = activeEvent.getEventComments();

        Assert.assertEquals(0, comments.size());
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setEventManager(new MockEventManager(true));
        return lifecycle;
    }
}
