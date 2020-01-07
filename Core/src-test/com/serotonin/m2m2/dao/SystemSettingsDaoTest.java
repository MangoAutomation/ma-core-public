/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.dao;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.db.MySQLProxy;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

/**
 * Test to prove there is a deadlock issue with MySQL and the SystemSettingsDao.
 *
 * NOTE: This does not effect H2
 *
 * @author Terry Packer
 *
 */
public class SystemSettingsDaoTest {

    private static final String dbUrl = "jdbc:mysql://localhost/mango37";
    private static final String dbUser = "mango";
    private static final String dbPass = "mango";

    @BeforeClass
    public static void setupDatabaseProxy() {
        MockMangoProperties properties = new MockMangoProperties();
        properties.setProperty("db.url", dbUrl);
        properties.setProperty("db.username", dbUser);
        properties.setProperty("db.password", dbPass);

        Providers.add(MangoProperties.class, properties);

        Common.databaseProxy = new MySQLProxy();
        Common.databaseProxy.initialize(SystemSettingsDaoTest.class.getClassLoader());

        Providers.add(IMangoLifecycle.class, new MockMangoLifecycle(new ArrayList<>()));
    }

    @Before
    public void cleanTable() {
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(Common.databaseProxy.getDataSource());
        ejt.update("DELETE FROM systemSettings WHERE settingName LIKE 'deadlockTest%'");
    }
    @After
    public void after() {
        cleanTable();
    }

    /**
     * This test proves that there is no deadlock when a value already exists in the
     *   system settings table
     */
    //@Test
    public void testDeadlockOnSetExistingValue() {

        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(Common.databaseProxy.getDataSource());

        AtomicReference<Exception> failed = new AtomicReference<>();
        AtomicInteger running = new AtomicInteger();
        AtomicInteger id = new AtomicInteger();
        for(int i=0; i<2; i++) {
            new Thread() {
                private int value = id.getAndIncrement();
                @Override
                public void run() {
                    try { Thread.sleep(100); } catch (InterruptedException e) { }
                    try{
                        ejt.update("INSERT INTO systemSettings VALUES ('deadlockTest" + value + "','value" + value + "')");
                        SystemSettingsDao.instance.setValue("deadlockTest" + value, "value" + value);
                    }catch(Exception e) {
                        failed.set(e);
                        return;
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();
            running.incrementAndGet();
        }
        int wait = 10;
        while(wait > 0) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
            if(failed.get() != null) {
                break;
            }
            if(running.get() == 0) {
                break;
            }
            wait--;
        }
        if(failed.get() != null) {
            failed.get().printStackTrace();
            fail(failed.get().getMessage());
        }
    }

    /**
     * This test proves that with > 1 thread there is deadlock when
     *   a key is not present in the table and the value is set
     */
    //@Test
    public void testDeadlockOnSetMissingValue() {

        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(Common.databaseProxy.getDataSource());


        AtomicReference<Exception> failed = new AtomicReference<>();
        AtomicInteger running = new AtomicInteger();
        AtomicInteger id = new AtomicInteger();
        for(int i=0; i<2; i++) {
            new Thread() {
                @Override
                public void run() {
                    int value = id.getAndIncrement();
                    try { Thread.sleep(100); } catch (InterruptedException e) { }
                    try{
                        SystemSettingsDao.instance.setValue("deadlockTest" + value, "value" + value);
                    }catch(Exception e) {
                        failed.set(e);
                        return;
                    }finally {
                        running.decrementAndGet();
                    }
                };
            }.start();
            running.incrementAndGet();
        }
        int wait = 10;
        while(wait > 0) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
            if(failed.get() != null) {
                break;
            }
            if(running.get() == 0) {
                break;
            }
            wait--;
        }
        if(failed.get() != null) {
            failed.get().printStackTrace();
            fail(failed.get().getMessage());
        }
    }

}
