/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.db.spring.ExtendedJdbcTemplate;

/**
 * Define upgrade actions that depend on upgrading to a specific version.  If
 * anything fails the upgrade will not happen and the module version store in Mango
 *  will not be updated so as to run again on next start.
 *
 * @author Terry Packer
 */
public abstract class UpgradeDefinition extends ModuleElementDefinition {

    @Autowired
    protected ExtendedJdbcTemplate ejt;

    @Autowired
    protected PlatformTransactionManager txManager;

    @Autowired
    protected DSLContext context;

    /**
     * Called after database is initialized, used to do any database related
     * upgrades outside of a schema definition.  If no exeption is thrown
     * the module is presumed to be running a the current version and that is
     * saved into the module versions table.
     *
     * One can expect the ejt field to be set and ready for use on the database at the time of this call
     *
     */
    public abstract void upgrade(Version previousVersion, Version current) throws Exception;

}
