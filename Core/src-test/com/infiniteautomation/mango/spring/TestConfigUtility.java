/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.EventDetectorsService;
import com.infiniteautomation.mango.spring.service.PublishedPointService;
import com.infiniteautomation.mango.spring.service.PublisherService;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.UpdateEventDetectorDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublishedPointVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherDefinition;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherVO;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * Provides convenient methods for creating mocked data sources, data points and publishers.
 *
 * @author Jared Wiltshire
 */
@Component
public final class TestConfigUtility {

    private final RoleService roleService;
    private final UsersService usersService;
    private final DataSourceService dataSourceService;
    private final DataPointService dataPointService;
    private final PublisherService publisherService;
    private final PublishedPointService publishedPointService;
    private final EventDetectorsService eventDetectorsService;
    private final Translations translations;

    @Autowired
    public TestConfigUtility(RoleService roleService, UsersService usersService, DataSourceService dataSourceService,
            DataPointService dataPointService, PublisherService publisherService,
            PublishedPointService publishedPointService, EventDetectorsService eventDetectorsService,
            Translations translations) {
        this.roleService = roleService;
        this.usersService = usersService;
        this.dataSourceService = dataSourceService;
        this.dataPointService = dataPointService;
        this.publisherService = publisherService;
        this.publishedPointService = publishedPointService;
        this.eventDetectorsService = eventDetectorsService;
        this.translations = translations;
    }

    public List<RoleVO> createRoles(int count) {
        return createRoles(count, UUID.randomUUID().toString());
    }

    public List<RoleVO> createRoles(int count, String prefix) {
        List<RoleVO> roles = new ArrayList<>();
        for(int i=0; i<count; i++) {
            roles.add(createRole(prefix + i, prefix + i));
        }
        return roles;
    }

    /**
     * Create a role
     */
    public RoleVO createRole(String xid, String name) {
        return createRole(xid, name, new Role[0]);
    }

    /**
     * Create a role with inherited roles (
     */
    public RoleVO createRole(String xid, String name, Role... inherited) {
        RoleVO role = new RoleVO(Common.NEW_ID, xid, name, new HashSet<>(Arrays.asList(inherited)));
        return roleService.insert(role);
    }

    /**
     * Create users with password=password and supplied permissions
     */
    public List<User> createUsers(int count, Role... roles){
        List<User> users = new ArrayList<>();
        for(int i=0; i<count; i++) {
            User user = createUser("User" + i,
                    "user" + i,
                    "password",
                    "user" + i + "@yourMangoDomain.com",
                    roles);
            users.add(user);
        }
        return users;
    }

    /**
     * Create a single user
     */
    public User createUser(String name, String username, String password, String email, Role... roles) {
        return createUser(Common.NEW_ID, name, username, password, email, roles);
    }

    /**
     * Create a user with pre-assigned ID
     */
    public User createUser(int id, String name, String username, String password, String email, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setUsername(username);
        user.setPassword(Common.encrypt(password));
        user.setEmail(email);
        user.setPhone("");
        user.setRoles(java.util.Set.of(roles));
        user.setDisabled(false);
        return usersService.insert(user);
    }

    public MockDataSourceVO createMockDataSource() {
        return createMockDataSource(UUID.randomUUID().toString(), UUID.randomUUID().toString(), false);
    }

    public MockDataSourceVO createMockDataSource(boolean enabled) {
        return createMockDataSource(UUID.randomUUID().toString(), UUID.randomUUID().toString(), enabled);
    }

    public MockDataSourceVO createMockDataSource(String name, String xid, boolean enabled) {
        return createMockDataSource(name, xid, enabled, new MangoPermission(), new MangoPermission());
    }

    public MockDataSourceVO createMockDataSource(String name, String xid, boolean enabled, MangoPermission readPermission, MangoPermission editPermission) {
        MockDataSourceVO vo = (MockDataSourceVO) ModuleRegistry.getDataSourceDefinition(MockDataSourceDefinition.TYPE_NAME).baseCreateDataSourceVO();
        vo.setXid(name);
        vo.setName(xid);
        vo.setEnabled(enabled);
        vo.setReadPermission(readPermission);
        vo.setEditPermission(editPermission);

        try {
            return (MockDataSourceVO) dataSourceService.insert(vo);
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(translations), e);
            return null;
        }
    }

    public List<DataPointVO> createMockDataPoints(MockDataSourceVO dataSource, int count) {
        List<DataPointVO> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(createMockDataPoint(dataSource, new MockPointLocatorVO()));
        }
        return points;
    }

    public List<IDataPoint> createMockDataPoints(int count) {
        List<IDataPoint> points = new ArrayList<>(count);
        MockDataSourceVO ds = createMockDataSource();
        for(int i=0; i<count; i++) {
            points.add(createMockDataPoint(ds, new MockPointLocatorVO()));
        }
        return points;
    }

    public List<IDataPoint> createMockDataPoints(int count, boolean enabled, MangoPermission readPermission, MangoPermission setPermission) {
        return createMockDataPoints(count, enabled, readPermission, setPermission, createMockDataSource(enabled));
    }

    public List<IDataPoint> createMockDataPoints(int count, boolean enabled, MangoPermission readPermission, MangoPermission setPermission, DataSourceVO ds) {
        List<IDataPoint> points = new ArrayList<>(count);
        for(int i=0; i<count; i++) {
            String name = UUID.randomUUID().toString();
            points.add(createMockDataPoint(Common.NEW_ID,
                    UUID.randomUUID().toString(),
                    name,
                    ds.getName() + " " + name,
                    enabled,
                    ds.getId(),
                    ds.getXid(),
                    readPermission,
                    setPermission,
                    new MockPointLocatorVO()));
        }
        return points;
    }

    public DataPointVO createMockDataPoint(MockDataSourceVO ds, MockPointLocatorVO vo) {
        return createMockDataPoint(ds, vo, false);
    }

    public DataPointVO createMockDataPoint(MockDataSourceVO ds, MockPointLocatorVO vo, boolean enabled) {
        String name = UUID.randomUUID().toString();
        return createMockDataPoint(Common.NEW_ID,
                UUID.randomUUID().toString(),
                name,
                ds.getName() + " " + name,
                enabled,
                ds.getId(),
                ds.getXid(),
                vo);
    }

    public DataPointVO createMockDataPoint(int id, String xid, String name,
            String deviceName, boolean enabled, int dataSourceId, String dataSourceXid, MockPointLocatorVO vo) {
        return createMockDataPoint(id, xid,
                name, deviceName, enabled, dataSourceId, dataSourceXid, new MangoPermission(), new MangoPermission(), vo);
    }

    public DataPointVO createMockDataPoint(int id, String xid, String name, String deviceName, boolean enabled, int dataSourceId,
            String dataSourceXid, MangoPermission readPermission, MangoPermission setPermission, MockPointLocatorVO vo) {

        DataPointVO dp = new DataPointVO();
        dp.setId(id);
        dp.setXid(xid);
        dp.setName(name);
        dp.setDeviceName(deviceName);
        dp.setEnabled(enabled);
        dp.setPointLocator(vo);
        dp.setDataSourceId(dataSourceId);
        dp.setDataSourceXid(dataSourceXid);
        dp.setReadPermission(readPermission);
        dp.setSetPermission(setPermission);

        try {
            return dataPointService.insert(dp);
        } catch (ValidationException e) {
            StringBuilder failureMessage = new StringBuilder();
            for (ProcessMessage m : e.getValidationResult().getMessages()) {
                String messagePart = m.getContextKey() + " -> " + m.getContextualMessage().translate(translations) + "\n";
                failureMessage.append(messagePart);
            }
            fail(failureMessage.toString(), e);
            return null;
        }
    }

    public DataPointVO createMockDataPoint(MockDataSourceVO ds) {
        return createMockDataPoint(ds, dp -> {});
    }

    public DataPointVO createMockDataPoint(MockDataSourceVO ds, DataType dataType, boolean enabled) {
        return createMockDataPoint(ds, new MockPointLocatorVO(dataType, true), enabled);
    }

    public DataPointVO createMockDataPoint(MockDataSourceVO ds, Consumer<DataPointVO> customizer) {
        DataPointVO dp = new DataPointVO();
        dp.setName(UUID.randomUUID().toString());
        dp.setDeviceName(ds.getName());
        dp.setPointLocator(new MockPointLocatorVO(DataType.NUMERIC, true));
        dp.setDataSourceId(ds.getId());

        customizer.accept(dp);

        try {
            return dataPointService.insert(dp);
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(translations), e);
            return null;
        }
    }

    /**
     * Create a publisher
     */
    public MockPublisherVO createMockPublisher(boolean enabled) {
        MockPublisherVO publisherVO = (MockPublisherVO) ModuleRegistry.getPublisherDefinition(MockPublisherDefinition.TYPE_NAME).baseCreatePublisherVO();
        publisherVO.setName(UUID.randomUUID().toString());
        publisherVO.setEnabled(enabled);
        try {
            return (MockPublisherVO) publisherService.insert(publisherVO);
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(translations), e);
            return null;
        }
    }

    /**
     * Create a publisher with points
     */
    public MockPublisherVO createMockPublisher(boolean enabled, List<MockPublishedPointVO> points) {
        MockPublisherVO publisherVO = (MockPublisherVO) ModuleRegistry.getPublisherDefinition(MockPublisherDefinition.TYPE_NAME).baseCreatePublisherVO();
        publisherVO.setName(UUID.randomUUID().toString());
        publisherVO.setEnabled(enabled);
        try {
            MockPublisherVO pub = (MockPublisherVO) publisherService.insert(publisherVO);
            for(MockPublishedPointVO point : points) {
                publishedPointService.insert(point);
            }
            return pub;
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(translations), e);
            return null;
        }
    }

    /**
     * Create a published point
     */
    public MockPublishedPointVO createMockPublishedPoint(MockPublisherVO publisher, IDataPoint dataPoint, boolean enabled) {
        MockPublishedPointVO pp = publisher.getDefinition().createPublishedPointVO(publisher, dataPoint);
        pp.setName(dataPoint.getName());
        pp.setEnabled(enabled);

        try {
            publishedPointService.insert(pp);
        }catch (ValidationException e) {
            fail(e.getValidationErrorMessage(translations), e);
            return null;
        }

        return pp;
    }

    /**
     * Create a list of published points
     */
    public List<PublishedPointVO> createMockPublishedPoints(MockPublisherVO publisher, List<IDataPoint> dataPoints, boolean enabled) {
        List<PublishedPointVO> points = new ArrayList<>();
        for(IDataPoint dp : dataPoints) {
            points.add(createMockPublishedPoint(publisher, dp, enabled));
        }
        return points;
    }

    public AbstractEventDetectorVO createMockEventDetector(DataPointVO dataPointVO) {
        return createMockEventDetector(dataPointVO, UpdateEventDetectorDefinition.TYPE_NAME);
    }

    public AbstractEventDetectorVO createMockEventDetector(DataPointVO dataPointVO, String type) {
        EventDetectorDefinition<?> eventDetectorDefinition = ModuleRegistry.getEventDetectorDefinition(type);
        try {
            return eventDetectorsService.insert(eventDetectorDefinition.baseCreateEventDetectorVO(dataPointVO.getId()));
        } catch (ValidationException e) {
            fail(e.getValidationErrorMessage(translations), e);
            return null;
        }
    }

    public List<AbstractEventDetectorVO> createMockEventDetectors(DataPointVO dataPointVO, int count) {
        return createMockEventDetectors(dataPointVO, UpdateEventDetectorDefinition.TYPE_NAME, count);
    }

    public List<AbstractEventDetectorVO> createMockEventDetectors(DataPointVO dataPointVO, String type, int count){
        List<AbstractEventDetectorVO> detectors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            detectors.add(createMockEventDetector(dataPointVO, type));
        }
        return detectors;
    }

}
