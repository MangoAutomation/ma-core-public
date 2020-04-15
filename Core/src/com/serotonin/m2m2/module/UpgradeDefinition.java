/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.module;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;

/**
 * Define upgrade actions that depend on upgrading to a specific version.  If
 * anything fails the upgrade will not happen and the module version store in Mango
 *  will not be updated so as to run again on next start.
 *
 * @author Terry Packer
 */
public abstract class UpgradeDefinition extends ModuleElementDefinition {

    //Core release versions
    protected final Version four = Version.valueOf("4.0.0");

    protected final ExtendedJdbcTemplate ejt;

    public UpgradeDefinition() {
        ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(Common.databaseProxy.getDataSource());
    }

    /**
     * Called after database is initialized, used to do any database related
     * upgrades outside of a schema definition.  If no exeption is thrown
     * the module is presumed to be running a the current version and that is
     * saved into the module versions table.
     * @param previousVersion
     * @param current
     */
    public abstract void upgrade(Version previousVersion, Version current) throws Exception;

}
