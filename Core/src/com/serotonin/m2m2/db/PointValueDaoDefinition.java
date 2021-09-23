/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.nio.file.Path;

import javax.annotation.PostConstruct;
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
     */
    @PostConstruct
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
     * Helper to get the database directory
     *
     * @return absolute path to databases directory
     */
    public Path getDatabasePath() {
        throw new UnsupportedOperationException();
    }

    /**
     * return this size of the database(s) in bytes
     * @return
     */
    public long getDatabaseSizeInBytes(){
        return DirectoryUtils.getSize(getDatabasePath().toFile()).getSize();
    }

}
