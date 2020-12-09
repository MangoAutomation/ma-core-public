/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db;

import java.io.File;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;
import com.serotonin.m2m2.db.dao.nosql.NoSQLDao;
import com.serotonin.m2m2.db.dao.nosql.NoSQLDataSerializer;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.util.DirectoryUtils;

/**
 * Base class for NoSQL Databases
 *
 * @author Terry Packer
 */
abstract public class NoSQLProxy extends ModuleElementDefinition {
    public abstract void initialize();
    public abstract void shutdown();

    public abstract PointValueDao createPointValueDao();

    public PointValueDao createPointValueDaoMetrics(){
        return new PointValueDaoMetrics(this.createPointValueDao());
    }


    /**
     * Helper to get the database directory
     *
     * @return Absolute path to databases directory ending in a slash
     */
    public static String getDatabasePath() {
        String location = Common.envProps.getString("db.nosql.location", "databases");
        return Common.MA_DATA_PATH.resolve(location).normalize().toString();
    }

    /**
     * Create a Dao for general NoSQL Storage
     *
     * @return
     */
    public abstract NoSQLDao createNoSQLDao(NoSQLDataSerializer serializer, String storeName);


    /**
     * return this size of the database(s) in bytes
     * @return
     */
    public long getDatabaseSizeInBytes(String storeName){
        return DirectoryUtils.getSize(new File(getDatabasePath())).getSize();
    }

}
