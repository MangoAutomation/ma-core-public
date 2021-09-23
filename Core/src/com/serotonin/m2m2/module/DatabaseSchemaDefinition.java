/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.jooq.Table;
import org.springframework.beans.factory.annotation.Autowired;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * A database schema definition allows a module to create an manage database tables and other objects as necessary to
 * perform its functionality.
 *
 * IMPORTANT: any tables with foreign keys into core tables MUST have an "on delete cascade" clause. FKs that reference
 * non-PK fields MUST also have an "on update cascade" clause. Failure to do this will result in esoteric error messages
 * presented to users, and the final blame for such being assigned to your module. Failure to fix such conditions will
 * result in bad module karma, if not outright module removal.
 *
 * @author Matthew Lohbihler
 */
abstract public class DatabaseSchemaDefinition extends ModuleElementDefinition {

    @Autowired
    protected DatabaseProxy databaseProxy;

    /**
     * Modules should add return all tables they manage. The names are used to perform conversions
     * between one type of database (e.g. Derby) and another (e.g. MySQL).
     *
     * @return tables to convert
     */
    abstract public List<Table<?>> getTablesForConversion();

    /**
     * The Java package in which upgrade classes can be found. An upgrade class must be provided whenever the "code
     * version" (see below) changes, and must be named Upgrade&lt;version&gt;, where &lt;version&gt; is the version
     * <b>from which</b> the module is being upgraded. (For example, Upgrade1 will upgrade version 1 to the next version
     * - presumably, but not necessarily, 2.) Upgrade classes extend the DBUpgrade class.
     *
     * @return the package name where upgrade classes can be found
     */
    abstract public String getUpgradePackage();

    /**
     * The version of the database schema that the current code requires. This is compared with the version stored in
     * the database - which represents the version of the schema - and determines whether the database needs to be
     * upgraded. This is separated from the version of the module because a module upgrade often does not require
     * database changes.
     *
     * ONLY POSITIVE NUMBERS should be used as version numbers. The recommendation is to start at 1 and increase from
     * there.
     *
     * @return the database schema version number required by the current code.
     */
    abstract public int getDatabaseSchemaVersion();

    /**
     * The module will check for this table and if it does not exist the install scripts
     *  will be run
     * @return
     */
    abstract public String getNewInstallationCheckTableName();

    /**
     * Provides the module an opportunity to check if it is a new installation (typically by checking if a table that it
     * uses exists or not). Modules should perform any required installation tasks at this time.
     *
     * NOTE that the dao's are NOT available yet
     *
     * @param ejt
     *            the JDBC template that provides access to the database
     */
    public void newInstallationCheck(ExtendedJdbcTemplate ejt) {
        if (!databaseProxy.tableExists(ejt, getNewInstallationCheckTableName())) {
            try (InputStream input = getInstallScript()) {
                databaseProxy.runScript(input, null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Check and un-install if necessary
     */
    @Override
    public void postRuntimeManagerTerminate(boolean uninstall) {
        if(uninstall) {
            // Remove the database tables.
            try (InputStream input = getUninstallScript()) {
                databaseProxy.runScript(input, null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Get the install script path based on the database type
     *  assumes the scripts are within module at web/db/
     * @return
     */
    protected InputStream getInstallScript() {
        String scriptName = "createTables-" + databaseProxy.getType().name() + ".sql";
        InputStream resource = this.getClass().getResourceAsStream(scriptName);
        if (resource == null) {
            throw new ShouldNeverHappenException("Could not get script " + scriptName + " for class " + this.getClass().getName());
        }
        return resource;
    }

    /**
     * Get the un-install script path based on the database type
     *  assumes the scripts are within module at web/db/
     * @return
     */
    protected InputStream getUninstallScript() {
        String scriptName = "uninstall.sql";
        InputStream resource = this.getClass().getResourceAsStream(scriptName);
        if (resource == null) {
            throw new ShouldNeverHappenException("Could not get script " + scriptName + " for class " + this.getClass().getName());
        }
        return resource;
    }
}
