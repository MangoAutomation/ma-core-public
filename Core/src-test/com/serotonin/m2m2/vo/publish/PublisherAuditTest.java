/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.tables.Audit;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.spring.service.AuditEventService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockEventManager;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

/**
 *
 * @author Terry Packer
 */
public class PublisherAuditTest extends MangoTestBase {
    private RunAs runAs;

    @Before
    public void init() {
        this.runAs = Common.getBean(RunAs.class);
    }

    public PublisherAuditTest() {
    }

    @Test
    public void testPublisherAuditTrail() {
        PublisherDao dao = PublisherDao.getInstance();

        MockPublisherVO vo = createMockPublisher(false);

        //Update and delete
        vo.setName("New Name");
        dao.update(vo.getId(), vo);
        dao.delete(vo.getId());

        List<AuditEventInstanceVO> auditEvents = getAuditEvents(AuditEventType.TYPE_PUBLISHER, vo.getId());
        Assert.assertEquals(3, auditEvents.size());
        Assert.assertEquals("event.audit.extended.added", auditEvents.get(0).getMessage().getKey());
        Assert.assertEquals("event.audit.extended.changed", auditEvents.get(1).getMessage().getKey());
        Assert.assertEquals("event.audit.extended.deleted", auditEvents.get(2).getMessage().getKey());

        Assert.assertFalse(auditEvents.get(0).getContext().isEmpty()); // Insert has no empty context
        Assert.assertFalse(auditEvents.get(1).getContext().isEmpty()); // Update has no empty context
        Assert.assertTrue(auditEvents.get(2).getContext().isEmpty());
    }

    @Test
    public void testPublisherAuditTrailWithoutAnyChangesSystem() {
        PublisherDao dao = PublisherDao.getInstance();

        MockPublisherVO vo = createMockPublisher(false);

        //Update without any changes and delete
        dao.update(vo.getId(), vo);
        dao.delete(vo.getId());

        List<AuditEventInstanceVO> auditEvents = getAuditEvents(AuditEventType.TYPE_PUBLISHER, vo.getId());
        Assert.assertEquals(2, auditEvents.size());
        Assert.assertEquals("event.audit.extended.added", auditEvents.get(0).getMessage().getKey());
        Assert.assertEquals("event.audit.extended.deleted", auditEvents.get(1).getMessage().getKey());

        Assert.assertFalse(auditEvents.get(0).getContext().isEmpty()); // Insert has no empty context
        Assert.assertTrue(auditEvents.get(1).getContext().isEmpty());
    }

    @Test
    public void testPublisherAuditTrailWithoutAnyChangesUser() {
        PublisherDao dao = PublisherDao.getInstance();

        MockPublisherVO vo = createMockPublisher(false);

        User admin = UserDao.getInstance().getByXid("admin");
        runAs.runAs(admin, () -> {
            //Update without any changes and delete
            dao.update(vo.getId(), vo);
            dao.delete(vo.getId());
        });

        List<AuditEventInstanceVO> auditEvents = getAuditEvents(AuditEventType.TYPE_PUBLISHER, vo.getId());
        Assert.assertEquals(3, auditEvents.size());
        Assert.assertEquals("event.audit.extended.added", auditEvents.get(0).getMessage().getKey());
        Assert.assertEquals("event.audit.extended.changed", auditEvents.get(1).getMessage().getKey());
        Assert.assertEquals("event.audit.extended.deleted", auditEvents.get(2).getMessage().getKey());

        Assert.assertFalse(auditEvents.get(0).getContext().isEmpty()); // Insert has no empty context
        Assert.assertTrue(auditEvents.get(1).getContext().isEmpty());
        Assert.assertTrue(auditEvents.get(2).getContext().isEmpty());
    }

    @Test
    public void testPublishedPointAuditTrail() {
        PublishedPointDao dao = PublishedPointDao.getInstance();

        IDataPoint dp = createMockDataPoints(1).get(0);
        MockPublisherVO publisher = createMockPublisher(false);
        MockPublishedPointVO vo = publisher.getDefinition().createPublishedPointVO(publisher, dp);
        vo.setName(dp.getName());
        vo.setEnabled(false);
        dao.insert(vo);

        //Update and delete
        vo.setName("New Name");
        dao.update(vo.getId(), vo);
        dao.delete(vo.getId());

        List<AuditEventInstanceVO> auditEvents = getAuditEvents(AuditEventType.TYPE_PUBLISHED_POINT, vo.getId());
        Assert.assertEquals(3, auditEvents.size());
    }

    protected List<AuditEventInstanceVO> getAuditEvents(String typeName, int objectId) {
        Audit auditTable = Audit.AUDIT;
        Condition conditions = DSL.and(auditTable.typeName.eq(typeName), auditTable.objectId.eq(objectId));
        ConditionSortLimit c = new ConditionSortLimit(conditions, Collections.singletonList(auditTable.id.asc()), null, null);

        List<AuditEventInstanceVO> events = new ArrayList<>();
        AuditEventService service = Common.getBean(AuditEventService.class);

        //Since these are raised in the background processing thread they may have not yet fired
        int retries = 4;
        while(retries > 0) {
            events.clear();
            service.customizedQuery(c, events::add);
            if (events.size() == 3) {
                break;
            }
            retries--;
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
        }
        return events;
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setEventManager(new MockEventManager(true));
        return lifecycle;
    }

}
