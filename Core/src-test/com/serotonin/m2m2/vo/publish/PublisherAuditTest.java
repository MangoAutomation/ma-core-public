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
import org.junit.BeforeClass;
import org.junit.Test;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.tables.Audit;
import com.infiniteautomation.mango.spring.service.AuditEventService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockEventManager;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherDefinition;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;

/**
 *
 * @author Terry Packer
 */
public class PublisherAuditTest extends MangoTestBase {

    private int pointCount = 100;

    public PublisherAuditTest() {
    }

    @BeforeClass
    public static void addDefinitions() {
        addModule("mockPublisher", new MockPublisherDefinition());
    }

    @Test
    public void testPublisherAuditTrail() {

        PublisherDao dao = PublisherDao.getInstance();

        MockPublisherVO vo = new MockPublisherVO();
        vo.setName("Name");
        vo.setDefinition(new MockPublisherDefinition());
        vo.setXid("PUB_TEST1");
        vo.setPoints(createPoints());
        vo.setEnabled(false);
        dao.insert(vo);

        //Modify and re-save
        vo.setName("New Name");
        dao.update(vo.getId(), vo);

        //Since these are raised in the background processing thread they may have not yet fired
        int retries = 4;
        List<AuditEventInstanceVO> events = new ArrayList<>();
        while(retries > 0) {
            AuditEventService service = Common.getBean(AuditEventService.class);
            Audit auditTable = Audit.AUDIT;
            Condition conditions = DSL.and(auditTable.typeName.eq(AuditEventType.TYPE_PUBLISHER), auditTable.objectId.eq(vo.getId()));
            ConditionSortLimit c = new ConditionSortLimit(conditions, Collections.singletonList(auditTable.id.asc()), null, null);
            service.customizedQuery(
                    c,
                    (evt) -> {
                        events.add(evt);
                    });
            if (events.size() == 2) {
                break;
            } else {
                events.clear();
            }
            retries--;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        Assert.assertEquals(2, events.size());
    }

    protected List<MockPublishedPointVO> createPoints(){
        List<MockPublishedPointVO> points = new ArrayList<MockPublishedPointVO>(pointCount);

        //Create data source
        MockDataSourceVO ds = createMockDataSource();

        for(int i=1; i<pointCount + 1; i++) {
            DataPointVO dp = createMockDataPoint(ds, new MockPointLocatorVO(DataTypes.NUMERIC, true));
            MockPublishedPointVO vo = new MockPublishedPointVO();
            vo.setDataPointId(dp.getId());
            points.add(vo);
        }
        return points;
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.setEventManager(new MockEventManager(true));
        return lifecycle;
    }

}
