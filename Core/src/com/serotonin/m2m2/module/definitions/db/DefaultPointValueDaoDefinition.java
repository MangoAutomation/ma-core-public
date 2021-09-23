/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.db;

import org.springframework.beans.factory.annotation.Autowired;

import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.PointValueDaoDefinition;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;

public class DefaultPointValueDaoDefinition extends PointValueDaoDefinition {

    @Autowired
    DatabaseProxy databaseProxy;

    PointValueDao pointValueDao;

    @Override
    public void initialize() {
        this.pointValueDao = new PointValueDaoSQL(databaseProxy);
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
