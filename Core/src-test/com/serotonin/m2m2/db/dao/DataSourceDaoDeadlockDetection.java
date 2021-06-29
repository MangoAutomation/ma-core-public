/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.infiniteautomation.mango.spring.service.DataPointService;
import com.infiniteautomation.mango.spring.service.DataSourceService;
import com.infiniteautomation.mango.spring.service.EventHandlerService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.RoleService;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.H2InMemoryDatabaseProxy;
import com.serotonin.m2m2.module.definitions.event.handlers.ProcessEventHandlerDefinition;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventTypeMatcher;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.MockPointLocatorVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 *
 * @author Terry Packer
 */
public class DataSourceDaoDeadlockDetection extends MangoTestBase {

    static final Log LOG = LogFactory.getLog(DataSourceDaoDeadlockDetection.class);

    /**
     * See the deadlock when you insert data source, then point,
     *  then delete point (outside of transaction) then delete source
     */
    @Test
    public void detectDeadlockFromDataSourceDeleteDataPointInsertAndDelete() {
        //Create data source
        //Add data point
        //Delete data source
        //Create data source

        int numThreads = 5; //25;
        int numDataSources = 10; //100;

        PermissionService permissionService = Common.getBean(PermissionService.class);
        DataSourceService dataSourceService = Common.getBean(DataSourceService.class);
        DataPointService dataPointService = Common.getBean(DataPointService.class);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger running = new AtomicInteger(numThreads);
        MutableObject<Exception> failure = new MutableObject<>(null);

        for(int i=0; i<numThreads; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < numDataSources; i++) {
                            //Create data source
                            MockDataSourceVO ds = new MockDataSourceVO();
                            ds.setName(Common.generateXid("Mock "));

                            dataSourceService.insert(ds);

                            //Create and save point
                            DataPointVO dp = createMockDataPoint(ds, new MockPointLocatorVO());

                            //Delete point
                            dataPointService.delete(dp);

                            //Delete data source
                            dataSourceService.delete(ds);

                            successes.getAndIncrement();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        failure.setValue(e);
                        failures.getAndIncrement();
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();
        }

        while(running.get() > 0) {
            try { Thread.sleep(100); }catch(Exception e) { }
        }
        if(failures.get() > 0) {
            fail("Ran " + successes.get() + " queries: " + failure.getValue().getMessage());
        }
    }

    /**
     * See the deadlock when you insert data source, then point,
     *  then then delete source (which deletes the point inside a transaction)
     */
    @Test
    public void detectDeadlockFromDataSourceDeleteDataPointInsert() {

        int numThreads = 5; //25;
        int numDataSources = 10; //100;

        PermissionService permissionService = Common.getBean(PermissionService.class);
        DataSourceService dataSourceService = Common.getBean(DataSourceService.class);

        AtomicInteger failures = new AtomicInteger();
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger running = new AtomicInteger(numThreads);
        MutableObject<Exception> failure = new MutableObject<>(null);

        for(int i=0; i<numThreads; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < numDataSources; i++) {
                            //Create data source
                            MockDataSourceVO ds = new MockDataSourceVO();
                            ds.setName(Common.generateXid("Mock "));

                            dataSourceService.insert(ds);

                            //Create and save point
                            createMockDataPoint(ds, new MockPointLocatorVO());

                            //Delete data source
                            dataSourceService.delete(ds);
                            successes.getAndIncrement();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        failure.setValue(e);
                        failures.getAndIncrement();
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();
        }

        while(running.get() > 0) {
            try { Thread.sleep(100); }catch(Exception e) { }
        }
        if(failures.get() > 0) {
            fail("Ran " + successes.get() + " queries: " + failure.getValue().getMessage());
        }
    }

    @Test
    public void detectDeadlockFromDataSourceDataPointInsert() {

        int numThreads = 5; //25;
        int numDataSources = 10; //100;

        PermissionService permissionService = Common.getBean(PermissionService.class);
        DataSourceService dataSourceService = Common.getBean(DataSourceService.class);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger running = new AtomicInteger(numThreads);
        MutableObject<Exception> failure = new MutableObject<>(null);

        for(int i=0; i<numThreads; i++) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < numDataSources; i++) {
                            //Create data source
                            MockDataSourceVO ds = new MockDataSourceVO();
                            ds.setName(Common.generateXid("Mock "));

                            dataSourceService.insert(ds);

                            //Create and save point
                            createMockDataPoint(ds, new MockPointLocatorVO());

                            successes.getAndIncrement();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        failure.setValue(e);
                        failures.getAndIncrement();
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();
        }

        while(running.get() > 0) {
            try { Thread.sleep(100); }catch(Exception e) { }
        }
        if(failures.get() > 0) {
            fail("Ran " + successes.get() + " queries: " + failure.getValue().getMessage());
        }
    }

    @Test
    public void detectDeadlockWithEventHandlerRoleMappingandDataSourceTablesUsingDaos() {

        //This will create 2x threads for each operating as one of the desired problem scenarios
        int numThreads = 5; //25;
        int numDataSources = 10; //100;
        AtomicInteger running = new AtomicInteger(numThreads * 2);

        PermissionService permissionService = Common.getBean(PermissionService.class);

        DataSource dataSource = Common.databaseProxy.getDataSource();
        JdbcConnectionPool pool = (JdbcConnectionPool)dataSource;
        pool.setMaxConnections(numThreads*100);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        MutableObject<Exception> failure = new MutableObject<>(null);

        for(int i=0; i<numThreads; i++) {
            //#5 lock eventHandlerMappings and roleMappings and then try to lock dataSources
            // Basically delete a data source
            new Thread() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < numDataSources; i++) {

                            //Insert an event handler
                            EventHandlerService eventHandlerService = Common.getBean(EventHandlerService.class);
                            ProcessEventHandlerVO eh = new ProcessEventHandlerVO();
                            eh.setDefinition(new ProcessEventHandlerDefinition());
                            eh.setName(Common.generateXid("Handler "));
                            eh.setActiveProcessCommand("ls");

                            eventHandlerService.insert(eh);

                            ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
                            ejt.setDataSource(dataSource);

                            //Get event handler
                            AbstractEventHandlerVO myEventHandler = eventHandlerService.get(eh.getXid());

                            //Create data source
                            MockDataSourceVO ds = new MockDataSourceVO();
                            ds.setName(Common.generateXid("Mock "));

                            DataSourceService dataSourceService = Common.getBean(DataSourceService.class);
                            dataSourceService.insert(ds);

                            //Insert a mapping
                            myEventHandler.setEventTypes(Collections.singletonList(new EventTypeMatcher(new DataSourceEventType(ds.getId(), ds.getPollAbortedExceptionEventId()))));
                            eventHandlerService.update(eh.getXid(), myEventHandler);

                            dataSourceService.delete(ds);

                            successes.getAndIncrement();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        failure.setValue(e);
                        failures.getAndIncrement();
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();

            //#8 lock dataSources and try to lock roleMappings
            //Basically update a data source
            new Thread() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < numDataSources; i++) {
                            ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
                            ejt.setDataSource(dataSource);

                            //Insert an event handler
                            EventHandlerService eventHandlerService = Common.getBean(EventHandlerService.class);
                            ProcessEventHandlerVO eh = new ProcessEventHandlerVO();
                            eh.setDefinition(new ProcessEventHandlerDefinition());
                            eh.setName(Common.generateXid("Handler "));
                            eh.setActiveProcessCommand("ls");

                            eventHandlerService.insert(eh);

                            //Get event handler
                            AbstractEventHandlerVO myEventHandler = eventHandlerService.get(eh.getXid());

                            //Create data source
                            MockDataSourceVO ds = new MockDataSourceVO();
                            ds.setName(Common.generateXid("Mock "));

                            DataSourceService dataSourceService = Common.getBean(DataSourceService.class);
                            dataSourceService.insert(ds);

                            //Insert a mapping
                            myEventHandler.setEventTypes(Collections.singletonList(new EventTypeMatcher(new DataSourceEventType(ds.getId(), ds.getPollAbortedExceptionEventId()))));
                            eventHandlerService.update(eh.getXid(), myEventHandler);

                            ds.setXid(ds.getXid() + 1);
                            dataSourceService.update(ds.getId(), ds);

                            successes.getAndIncrement();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        failure.setValue(e);
                        failures.getAndIncrement();
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();
        }

        while(running.get() > 0) {
            try { Thread.sleep(100); }catch(Exception e) { }
        }
        if(failures.get() > 0) {
            fail("Ran " + successes.get() + " queries: " + failure.getValue().getMessage());
        }
    }

    @Test
    public void detectDeadlockWithEventHandlerRoleMappingandDataSourceTablesExplicit() {

        //This will create 2x threads for each operating as one of the desired problem scenarios
        int numThreads = 5;
        int numDataSources = 10;
        AtomicInteger running = new AtomicInteger(numThreads * 2);

        PermissionService permissionService = Common.getBean(PermissionService.class);

        //Insert some roles
        int roleCount = 0;
        RoleService roleService = Common.getBean(RoleService.class);
        List<RoleVO> roleVOs = new ArrayList<>();
        Set<Role> roles = new HashSet<>();
        for(int i=0; i<roleCount; i++) {
            RoleVO role = new RoleVO(Common.NEW_ID, Common.generateXid("ROLE_"), "Role " + i);
            roleVOs.add(role);
            roleService.insert(role);
            roles.add(role.getRole());
        }

        DataSource dataSource = Common.databaseProxy.getDataSource();
        JdbcConnectionPool pool = (JdbcConnectionPool)dataSource;
        pool.setMaxConnections(numThreads*2);

        PlatformTransactionManager transactionManager = Common.databaseProxy.getTransactionManager();

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        MutableObject<Exception> failure = new MutableObject<>(null);

        for(int i=0; i<numThreads; i++) {
            //#5 lock eventHandlerMappings and roleMappings and then try to lock dataSources
            // Basically delete a data source
            new Thread() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < numDataSources; i++) {

                            //Insert an event handler
                            EventHandlerService eventHandlerService = Common.getBean(EventHandlerService.class);
                            ProcessEventHandlerVO eh = new ProcessEventHandlerVO();
                            eh.setDefinition(new ProcessEventHandlerDefinition());
                            eh.setName(Common.generateXid("Handler "));
                            eh.setActiveProcessCommand("ls");

                            eventHandlerService.insert(eh);

                            ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
                            ejt.setDataSource(dataSource);

                            //Get event handler
                            AbstractEventHandlerVO myEventHandler = eventHandlerService.get(eh.getXid());

                            //Create data source
                            MockDataSourceVO ds = new MockDataSourceVO();
                            ds.setName(Common.generateXid("Mock "));

                            DataSourceService dataSourceService = Common.getBean(DataSourceService.class);
                            dataSourceService.insert(ds);

                            //Insert a mapping
                            myEventHandler.setEventTypes(Collections.singletonList(new EventTypeMatcher(new DataSourceEventType(ds.getId(), ds.getPollAbortedExceptionEventId()))));
                            eventHandlerService.update(eh.getXid(), myEventHandler);

                            new TransactionTemplate(transactionManager).execute((status) -> {
                                //The order of these statements matters for deadlock, we must always lock groups of tables in the same order
                                ejt.update("DELETE FROM dataSources WHERE id=?", new Object[]{ds.getId()});
                                ejt.update("DELETE FROM eventHandlersMapping WHERE eventTypeName=? AND eventTypeRef1=?", new Object[]{EventType.EventTypeNames.DATA_SOURCE, ds.getId()});

                                return null;
                            });
                            successes.getAndIncrement();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        failure.setValue(e);
                        failures.getAndIncrement();
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();

            //#8 lock dataSources and try to lock roleMappings
            //Basically update a data source
            new Thread() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < numDataSources; i++) {
                            ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
                            ejt.setDataSource(dataSource);

                            //Insert an event handler
                            EventHandlerService eventHandlerService = Common.getBean(EventHandlerService.class);
                            ProcessEventHandlerVO eh = new ProcessEventHandlerVO();
                            eh.setDefinition(new ProcessEventHandlerDefinition());
                            eh.setName(Common.generateXid("Handler "));
                            eh.setActiveProcessCommand("ls");

                            eventHandlerService.insert(eh);

                            //Get event handler
                            AbstractEventHandlerVO myEventHandler = eventHandlerService.get(eh.getXid());

                            //Create data source
                            MockDataSourceVO ds = new MockDataSourceVO();
                            ds.setName(Common.generateXid("Mock "));

                            DataSourceService dataSourceService = Common.getBean(DataSourceService.class);
                            dataSourceService.insert(ds);

                            //Insert a mapping
                            myEventHandler.setEventTypes(Collections.singletonList(new EventTypeMatcher(new DataSourceEventType(ds.getId(), ds.getPollAbortedExceptionEventId()))));
                            eventHandlerService.update(eh.getXid(), myEventHandler);

                            new TransactionTemplate(transactionManager).execute((status) -> {
                                ejt.update("UPDATE dataSources SET xid=? WHERE id=?", new Object[]{ds.getXid() + "1", ds.getId()});
                                return null;
                            });
                            successes.getAndIncrement();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        failure.setValue(e);
                        failures.getAndIncrement();
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();
        }

        while(running.get() > 0) {
            try { Thread.sleep(100); }catch(Exception e) { }
        }
        if(failures.get() > 0) {
            fail("Ran " + successes.get() + " queries: " + failure.getValue().getMessage());
        }
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();

        boolean enableWebConsole = Common.envProps.getBoolean("db.web.start");
        int webPort = Common.envProps.getInt("db.web.port");
        lifecycle.setDb(new H2InMemoryDatabaseProxyNoLocking(enableWebConsole, webPort));

        return lifecycle;
    }

    private class H2InMemoryDatabaseProxyNoLocking extends H2InMemoryDatabaseProxy {
        public H2InMemoryDatabaseProxyNoLocking(boolean initWebConsole, Integer webPort) {
            super(initWebConsole, webPort, false);
        }
        @Override
        public String getUrl() {
            return "jdbc:h2:mem:" + databaseName + ";MV_STORE=FALSE;DB_CLOSE_ON_EXIT=FALSE";
        }
    }

}
