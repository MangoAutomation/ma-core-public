/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.publish;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.infiniteautomation.mango.spring.db.AuditEventTableDefinition;
import com.infiniteautomation.mango.spring.service.AuditEventService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockEventManager;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;
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
        List<ModuleElementDefinition> definitions = new ArrayList<>();
        definitions.add(new MockPublisherDefinition());
        addModule("mockPublisher", definitions);
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

        List<AuditEventInstanceVO> events = new ArrayList<>();
        AuditEventService service = Common.getBean(AuditEventService.class);
        AuditEventTableDefinition table = Common.getBean(AuditEventTableDefinition.class);
        SelectJoinStep<Record> query = service.getJoinedSelectQuery();
        service.customizedQuery(query.where(
                DSL.and(table.getAlias("typeName").eq(AuditEventType.TYPE_PUBLISHER), table.getAlias("objectId").eq(vo.getId()))).orderBy(table.getAlias("id")),
                (evt, index) -> {
                    events.add(evt);
                });
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
