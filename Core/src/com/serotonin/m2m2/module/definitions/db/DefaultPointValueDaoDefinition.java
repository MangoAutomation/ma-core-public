/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.db;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.PointValueDaoDefinition;
import com.serotonin.m2m2.db.dao.DataPointDao;
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
    @Autowired
    DataPointDao dataPointDao;

    PointValueDao pointValueDao;

    @Override
    public void initialize() {
        int chunkSize = env.getProperty("db.default.chunkSize", int.class, 16_384);
        this.pointValueDao = new PointValueDaoSQL(databaseProxy, monitoredValues, chunkSize, systemSettingsDao, dataPointDao);
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
        return Common.envProps.getInt("db.default.order", Integer.MAX_VALUE);
    }
}
