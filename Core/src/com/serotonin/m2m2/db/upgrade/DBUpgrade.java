/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.BaseDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.util.StringUtils;

/**
 * Base class for instances that perform database upgrades. The naming of subclasses follows the convention
 * 'Upgrade[version]', where '[version]' is the version that the class upgrades <b>from</b>. The subclass must be in
 * this package.
 * 
 * @author Matthew Lohbihler
 */
abstract public class DBUpgrade extends BaseDao {
    private static final Log LOG = LogFactory.getLog(DBUpgrade.class);
    protected static final String DEFAULT_DATABASE_TYPE = "*";

    public static void checkUpgrade() {
        checkUpgrade(SystemSettingsDao.DATABASE_SCHEMA_VERSION, Common.getDatabaseSchemaVersion(), DBUpgrade.class
                .getPackage().getName(), "core", DBUpgrade.class.getClassLoader());
        LOG.info("Starting instance with core version " + Common.getVersion() + ", db "
                + Common.getDatabaseSchemaVersion());
    }

    public static void checkUpgrade(DatabaseSchemaDefinition def, ClassLoader classLoader) {
        String name = def.getModule().getName();
        checkUpgrade(SystemSettingsDao.DATABASE_SCHEMA_VERSION + "." + name, def.getDatabaseSchemaVersion(),
                def.getUpgradePackage(), name, classLoader);
    }

    public static void checkUpgrade(String settingsKey, int codeVersion, String pkg, String moduleName,
            ClassLoader classLoader) {
        // If this is a very old version of the system, there may be multiple upgrades to run, so start a loop.
        while (true) {
            // Get the current schema version.
            int schemaVersion = SystemSettingsDao.getIntValue(settingsKey, -1);

            if (schemaVersion == -1) {
                if ("core".equals(moduleName))
                    // Probably an old core. Assume the version to be 1 to do complete upgrade
                    schemaVersion = 1;
                else {
                    // Probably a new module. Put the current code version into the database.
                    new SystemSettingsDao().setIntValue(settingsKey, codeVersion);
                    schemaVersion = codeVersion;
                }
            }

            // Convert the schema version to the class name convention. This simply means replacing dots with
            // underscores and prefixing 'Upgrade' and this package.
            String upgradeClassname = pkg + ".Upgrade" + Integer.toString(schemaVersion);

            // See if there is a class with this name.
            Class<?> clazz = null;
            DBUpgrade upgrade = null;
            try {
                clazz = Class.forName(upgradeClassname, true, classLoader);
            }
            catch (ClassNotFoundException e) {
                // no op
            }

            if (clazz != null) {
                try {
                    upgrade = (DBUpgrade) clazz.newInstance();
                }
                catch (Exception e) {
                    // Should never happen so wrap in a runtime and rethrow.
                    throw new ShouldNeverHappenException(e);
                }
            }

            if (upgrade == null) {
                if (schemaVersion != codeVersion)
                    LOG.warn("The code version " + codeVersion + " of module " + moduleName
                            + " does not match the schema version " + schemaVersion);
                break;
            }

            try {
                LOG.warn("Upgrading '" + moduleName + "' from " + schemaVersion + " to "
                        + upgrade.getNewSchemaVersion());
                upgrade.upgrade();
                new SystemSettingsDao().setValue(settingsKey, upgrade.getNewSchemaVersion());
            }
            catch (Exception e) {
                throw new ShouldNeverHappenException(e);
            }
        }
    }

    abstract protected void upgrade() throws Exception;

    abstract protected String getNewSchemaVersion();

    /**
     * Convenience method for subclasses
     * 
     * @param script
     *            the array of script lines to run
     * @param out
     *            the stream to which to direct output from running the script
     * @throws Exception
     *             if something bad happens
     */
    protected void runScript(String[] script) throws Exception {
        OutputStream out = createUpdateLogOutputStream();
        Common.databaseProxy.runScript(script, out);
        out.flush();
        out.close();
    }

    protected void runScript(String[] script, OutputStream out) throws Exception {
        Common.databaseProxy.runScript(script, out);
    }

    protected void runScript(Map<String, String[]> scripts) throws Exception {
        OutputStream out = createUpdateLogOutputStream();
        runScript(scripts, out);
        out.flush();
        out.close();
    }

    protected void runScript(Map<String, String[]> scripts, final OutputStream out) throws Exception {
        String[] script = scripts.get(Common.databaseProxy.getType().name());
        if (script == null)
            script = scripts.get(DEFAULT_DATABASE_TYPE);
        runScript(script, out);
    }

    protected OutputStream createUpdateLogOutputStream() {
        String dir = Common.envProps.getString("db.update.log.dir", "");
        dir = StringUtils.replaceMacros(dir, System.getProperties());

        File logDir = new File(dir);
        File logFile = new File(logDir, getClass().getName() + ".log");
        LOG.info("Writing upgrade log to " + logFile.getAbsolutePath());

        try {
            if (logDir.isDirectory() && logDir.canWrite())
                return new FileOutputStream(logFile);
        }
        catch (Exception e) {
            LOG.error("Failed to create database upgrade log file.", e);
        }

        LOG.warn("Failing over to console for printing database upgrade messages");
        return System.out;
    }
}
