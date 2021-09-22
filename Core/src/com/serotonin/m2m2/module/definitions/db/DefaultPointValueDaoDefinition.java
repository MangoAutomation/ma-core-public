/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.db;

import javax.sql.DataSource;

import org.jooq.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.PointValueDaoDefinition;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;

public class DefaultPointValueDaoDefinition extends PointValueDaoDefinition {

    @Autowired
    DataSource dataSource;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    DatabaseType databaseType;
    @Autowired
    Configuration configuration;

    PointValueDao pointValueDao;

    @Override
    public void initialize() {
        this.pointValueDao = new PointValueDaoSQL(dataSource, transactionManager, databaseType, configuration);
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
