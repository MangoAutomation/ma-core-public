/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.db;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.PointValueDaoDefinition;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;

public class DefaultPointValueDaoDefinition extends PointValueDaoDefinition {

    @Autowired
    DatabaseProxy databaseProxy;
    @Autowired
    MonitoredValues monitoredValues;
    @Autowired
    SystemSettingsDao systemSettingsDao;

    PointValueDao pointValueDao;

    @Override
    public void initialize() {
        this.pointValueDao = new PointValueDaoSQL(databaseProxy, monitoredValues, systemSettingsDao);
    }

    @Override
    public void shutdown() {
        // no-op
    }

    @Override
    public PointValueDao getPointValueDao() {
        return pointValueDao;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
