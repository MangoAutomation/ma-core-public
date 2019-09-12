/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.dao;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.db.MySQLProxy;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;

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
        Common.envProps = new MockMangoProperties();
        Common.envProps.setDefaultValue("db.url", dbUrl);
        Common.envProps.setDefaultValue("db.username", dbUser);
        Common.envProps.setDefaultValue("db.password", dbPass);
        Common.databaseProxy = new MySQLProxy();
        Common.databaseProxy.initialize(SystemSettingsDaoTest.class.getClassLoader());
    }
    
    
    //@Test
    public void testDeadlockOnSetValue() {
        
        for(int i=0; i<100; i++) {
            AtomicInteger id = new AtomicInteger(i);
            new Thread() {
                public void run() {
                    try { Thread.sleep(100); } catch (InterruptedException e) { }
                    try{
                        SystemSettingsDao.instance.setValue("setting" + id.get(), "value" + id.get());
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                };
            }.start();
        }
        try { Thread.sleep(10000); } catch (InterruptedException e) { }
    }
    
}
