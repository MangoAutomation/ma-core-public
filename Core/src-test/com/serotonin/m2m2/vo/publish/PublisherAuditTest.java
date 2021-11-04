/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.Assert;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.tables.Audit;
import com.infiniteautomation.mango.spring.service.AuditEventService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockEventManager;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.PublishedPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

/**
 *
 * @author Terry Packer
 */
public class PublisherAuditTest extends MangoTestBase {

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
