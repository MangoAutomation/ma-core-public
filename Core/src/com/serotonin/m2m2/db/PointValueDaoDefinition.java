/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.nio.file.Path;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;
import com.serotonin.util.DirectoryUtils;

/**
 * Definition that provides a {@link PointValueDao} for accessing point values from a time series database.
 * @author Terry Packer
 */
abstract public class PointValueDaoDefinition extends ModuleElementDefinition {

    @Autowired
    protected Environment env;

    /**
     * Initialize the {@link PointValueDao}
     * Note: Only the highest priority definition should be initialized by {@link com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration#pointValueDao MangoRuntimeContextConfiguration}
     */
    //@PostConstruct
    public abstract void initialize();

    /**
     * Terminate the {@link PointValueDao}
     */
    @PreDestroy
    public abstract void shutdown();

    /**
     * @return a singleton, thread safe instance of the {@link PointValueDao} implementation
     */
    public abstract PointValueDao getPointValueDao();

    /**
     * @return absolute path to database directory
     * @throws UnsupportedOperationException if not supported.
     */
    public Path getDatabasePath() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return size of the database in bytes
     * @throws UnsupportedOperationException if not supported.
     */
    public long getDatabaseSizeInBytes(){
        return DirectoryUtils.getSize(getDatabasePath().toFile()).getSize();
    }

}
