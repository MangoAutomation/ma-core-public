/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.components.RunAs;
import com.infiniteautomation.mango.util.RQLUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogChangeEventDetectorDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

import net.jazdw.rql.parser.ASTNode;

public class EventInstanceWithTagsQueryTest extends MangoTestBase {

    private Map<String, String> allTags = new HashMap<>();

    private String key1;
    private String value1;
    private DataPointVO point1;
    private AbstractPointEventDetectorVO detector1;

    private String key2;
    private String value2;
    private DataPointVO point2;
    private AbstractPointEventDetectorVO detector2;

    private EventInstanceService service;
    private EventDetectorsService eventDetectorsService;
    private EventDao dao;
    private RoleService roleService;
    private RunAs runAs;

    private Role point1ReadRole;
    private Role point2ReadRole;
    private User point1User;
    private User point2User;
    private User allUser;

    private final Random random = new Random();

    @Before
    public void before(){
        super.before();
        service = Common.getBean(EventInstanceService.class);
        eventDetectorsService = Common.getBean(EventDetectorsService.class);
        dao = Common.getBean(EventDao.class);
        roleService = Common.getBean(RoleService.class);
        runAs = Common.getBean(RunAs.class);

        setupRoles();

        AtomicInteger count = new AtomicInteger();
        List<String> tagKeys = Stream.generate(() -> "key" + count.getAndIncrement())
                .limit(2).collect(Collectors.toList());
        allTags = tagKeys.stream().collect(Collectors.toMap(Function.identity(), k -> k + "_value" + random.nextInt(10)));

        MockDataSourceVO ds = createMockDataSource(true);

        //Insert 2 data points
        point1 = createMockDataPoint(ds, (dp) -> {
            key1 = tagKeys.get(0);
            value1 = allTags.get(tagKeys.get(0));
            Map<String, String> tags = new HashMap<>();
            tags.put(key1, value1);
            dp.setTags(tags);
            dp.setReadPermission(MangoPermission.requireAllRoles(point1ReadRole));
        });

        point2 = createMockDataPoint(ds, (dp) -> {
            key2 = tagKeys.get(1);
            value2 = allTags.get(tagKeys.get(1));
            Map<String, String> tags = new HashMap<>();
            tags.put(key2, value2);
            dp.setTags(tags);
            dp.setReadPermission(MangoPermission.requireAllRoles(point2ReadRole));
        });

        //Create the event detectors
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            AnalogChangeEventDetectorDefinition definition = (AnalogChangeEventDetectorDefinition)ModuleRegistry.getEventDetectorDefinition(AnalogChangeEventDetectorDefinition.TYPE_NAME);
            AnalogChangeDetectorVO detector = definition.baseCreateEventDetectorVO(point1);
            detector.setName("Detector for point " + point1.getName());
            eventDetectorsService.insert(detector);
            detector1 = detector;
        });
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            AnalogChangeEventDetectorDefinition definition = (AnalogChangeEventDetectorDefinition)ModuleRegistry.getEventDetectorDefinition(AnalogChangeEventDetectorDefinition.TYPE_NAME);
            AnalogChangeDetectorVO detector = definition.baseCreateEventDetectorVO(point2);
            detector.setName("Detector for point " + point2.getName());
            eventDetectorsService.insert(detector);
            detector2 = detector;
        });
    }

    @Test
    public void testTagQuery() {

        runAs.runAs(runAs.systemSuperadmin(), () -> {

            //Insert some events for the points
            insertEvents(point1, detector1, 5, AlarmLevels.URGENT);
            insertEvents(point2, detector2, 5, AlarmLevels.CRITICAL);
        });

        runAs.runAs(point1User, () -> {
            AtomicInteger count = new AtomicInteger();
            service.query(createRql(allTags, false), (evt) -> {
                count.getAndIncrement();
                Assert.assertEquals(point1.getId(), evt.getEventType().getReferenceId1());
                Assert.assertEquals(detector1.getId(), evt.getEventType().getReferenceId2());
            });

            Assert.assertEquals(5, count.get());
        });

    }

    @Test
    public void testTagQueryAsAdmin() {

        runAs.runAs(runAs.systemSuperadmin(), () -> {
            //Insert some events for the points
            insertEvents(point1, detector1, 5, AlarmLevels.URGENT);
            insertEvents(point2, detector2, 5, AlarmLevels.CRITICAL);

            List<String> keys = allTags.keySet().stream().collect(Collectors.toList());
            List<String> values = new ArrayList<>();
            keys.forEach(key -> values.add(allTags.get(key)));

            AtomicInteger count = new AtomicInteger();
            service.query(createRql(point1.getTags(), true), (evt) -> {
                count.getAndIncrement();
                Assert.assertEquals(point1.getId(), evt.getEventType().getReferenceId1());
                Assert.assertEquals(detector1.getId(), evt.getEventType().getReferenceId2());
            });

            Assert.assertEquals(5, count.get());
        });

    }

    @Test
    public void testCountDataPointEventCountsByRQL() {
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            //Insert some events for the points
            insertEvents(point1, detector1, 5, AlarmLevels.URGENT);
            insertEvents(point2, detector2, 5, AlarmLevels.CRITICAL);
        });

        runAs.runAs(point2User, () -> {
            //Create the query
            ASTNode ast = RQLUtils.parseRQLtoAST(createRql(allTags, false));
            int count = service.countDataPointEventCountsByRQL(ast, null, null);
            Assert.assertEquals(1, count);
        });
    }

    @Test
    public void testCountDataPointEventCountsByRQLAsAdmin() {
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            //Insert some events for the points
            insertEvents(point1, detector1, 5, AlarmLevels.URGENT);
            insertEvents(point2, detector2, 5, AlarmLevels.CRITICAL);

            //Create the query
            ASTNode ast = RQLUtils.parseRQLtoAST(createRql(allTags, false));
            int count = service.countDataPointEventCountsByRQL(ast, null, null);
            Assert.assertEquals(2, count);
        });
    }

    @Test
    public void testQueryDataPointEventCountsByRQL() {
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            //Insert some events for the points
            insertEvents(point1, detector1, 5, AlarmLevels.URGENT);
            insertEvents(point2, detector2, 5, AlarmLevels.CRITICAL);
        });

        runAs.runAs(point1User, () -> {

            //Create the query
            ASTNode ast = RQLUtils.parseRQLtoAST(createRql(allTags, false));
            AtomicInteger count = new AtomicInteger();
            service.queryDataPointEventCountsByRQL(ast, null, null, (row) -> {
                count.getAndIncrement();
                //What point is this row for
                if(row.getName().equals(point1.getName())) {
                    Assert.assertEquals(point1.getXid(), row.getXid());
                    Assert.assertEquals(point1.getName(), row.getName());
                    Assert.assertEquals(point1.getDeviceName(), row.getDeviceName());
                    Assert.assertEquals(AlarmLevels.URGENT, row.getAlarmLevel());
                    Assert.assertEquals(5, row.getCount());
                    //The tags map has all keys but only values for the point of this row
                    Assert.assertEquals(null, row.getTags().get(key2));
                    Assert.assertEquals(value1, row.getTags().get(key1));

                }else if(row.getName().equals(point2.getName())) {
                    Assert.fail("Should not get any rows for point 2");
                }
            });
            Assert.assertEquals(1, count.get());
        });
    }

    @Test
    public void testQueryDataPointEventCountsByRQLAsAdmin() {
        runAs.runAs(runAs.systemSuperadmin(), () -> {
            //Insert some events for the points
            insertEvents(point1, detector1, 5, AlarmLevels.URGENT);
            insertEvents(point2, detector2, 5, AlarmLevels.CRITICAL);

            //Create the query
            ASTNode ast = RQLUtils.parseRQLtoAST(createRql(allTags, false));
            AtomicInteger count = new AtomicInteger();
            service.queryDataPointEventCountsByRQL(ast, null, null, (row) -> {
                count.getAndIncrement();
                //What point is this row for
                if(row.getName().equals(point1.getName())) {
                    Assert.assertEquals(point1.getXid(), row.getXid());
                    Assert.assertEquals(point1.getName(), row.getName());
                    Assert.assertEquals(point1.getDeviceName(), row.getDeviceName());
                    Assert.assertEquals(AlarmLevels.URGENT, row.getAlarmLevel());
                    Assert.assertEquals(5, row.getCount());
                    //The tags map has all keys but only values for the point of this row
                    Assert.assertEquals(null, row.getTags().get(key2));
                    Assert.assertEquals(value1, row.getTags().get(key1));

                }else if(row.getName().equals(point2.getName())) {
                    Assert.assertEquals(point2.getXid(), row.getXid());
                    Assert.assertEquals(point2.getName(), row.getName());
                    Assert.assertEquals(point2.getDeviceName(), row.getDeviceName());
                    Assert.assertEquals(AlarmLevels.CRITICAL, row.getAlarmLevel());
                    Assert.assertEquals(5, row.getCount());
                    //The tags map has all keys but only values for the point of this row
                    Assert.assertEquals(null, row.getTags().get(key1));
                    Assert.assertEquals(value2, row.getTags().get(key2));
                }
            });
            Assert.assertEquals(2, count.get());
        });
    }

    /**
     * Create RQL from a set of tags
     * @param tags
     * @param and - should all tags be required on a point
     * @return
     */
    protected String createRql(Map<String, String> tags, boolean and) {
        return tags.entrySet()
                .stream()
                .map(entry -> "eq(tags." + entry.getKey() + "," + entry.getValue() + ")")
                .collect(Collectors.joining(and ? "&" : "|"));
    }


    protected List<EventInstance> insertEvents(DataPointVO point, AbstractPointEventDetectorVO detector, int count, AlarmLevels level) {
        List<EventInstance> list = new ArrayList<>();
        for(int i=0; i<count; i++) {
            EventInstance evt = createDataPointEventInstance(point, detector, level);
            evt.setReadPermission(point.getReadPermission());
            dao.saveEvent(evt);
            list.add(evt);
        }
        return list;
    }

    protected EventInstance createDataPointEventInstance(DataPointVO point, AbstractPointEventDetectorVO detector, AlarmLevels level) {

        DataPointEventType type = new DataPointEventType(point, detector);

        Map<String,Object> context = new HashMap<String, Object>();
        context.put("pointEventDetector", detector);
        context.put("point", point);

        EventInstance instance = new EventInstance(
                type,
                this.timer.currentTimeMillis(),
                true,
                level,
                new TranslatableMessage("common.default", "testing"),
                context);

        return instance;
    }

    void setupRoles() {
        roleService = Common.getBean(RoleService.class);

        //Add some roles
        RoleVO temp = new RoleVO(Common.NEW_ID, "point-1-read-role", "Role to allow reading.");
        roleService.insert(temp);
        point1ReadRole = new Role(temp);

        temp = new RoleVO(Common.NEW_ID, "point-2-read-role", "Role to allow reading.");
        roleService.insert(temp);
        point2ReadRole = new Role(temp);

        point1User = createUser("point1User", "point1User", "password", "point1User@example.com", point1ReadRole);
        point2User = createUser("poin2User", "poin2User", "password", "poin2User@example.com", point2ReadRole);
        allUser = createUser("allUser", "allUser", "password", "allUser@example.com", point1ReadRole, point2ReadRole);
    }
}
