/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.BaseDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * Base class for instances that perform database upgrades. The naming of subclasses follows the convention
 * 'Upgrade[version]', where '[version]' is the version that the class upgrades <b>from</b>. The subclass must be in
 * this package.
 *
 * <p>Ensure that you update {@link com.serotonin.m2m2.Common#getDatabaseSchemaVersion()} when implementing upgrades</p>
 *
 * @author Matthew Lohbihler
 */
abstract public class DBUpgrade extends BaseDao {
    private static final Log LOG = LogFactory.getLog(DBUpgrade.class);
    protected static final String DEFAULT_DATABASE_TYPE = "*";

    public static void checkUpgrade() {
        checkUpgrade(SystemSettingsDao.DATABASE_SCHEMA_VERSION, Common.getDatabaseSchemaVersion(), DBUpgrade.class
                .getPackage().getName(), ModuleRegistry.CORE_MODULE_NAME, DBUpgrade.class.getClassLoader());
        LOG.info("Starting instance with core version " + Common.getVersion() + ", schema v"
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
            int schemaVersion = SystemSettingsDao.instance.getIntValue(settingsKey, -1);

            if (schemaVersion == -1) {
                if (ModuleRegistry.CORE_MODULE_NAME.equals(moduleName))
                    // Probably an old core. Assume the version to be 1 to do complete upgrade
                    schemaVersion = 1;
                else {
                    // Probably a new module. Put the current code version into the database.
                    SystemSettingsDao.instance.setIntValue(settingsKey, codeVersion);
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
                SystemSettingsDao.instance.setValue(settingsKey, upgrade.getNewSchemaVersion());
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
    protected void runScript(String[] script) throws IOException {
        try (OutputStream out = createUpdateLogOutputStream()) {
            runScript(script, out);
        }
    }

    protected void runScript(String[] script, OutputStream out) {
        try {
            Common.databaseProxy.runScript(script, out);
        } catch (Exception e) {
            PrintWriter pw = new PrintWriter(out);
            e.printStackTrace(pw);
            pw.flush();
            throw e;
        }
    }

    protected void runScript(Map<String, String[]> scripts) throws IOException {
        try (OutputStream out = createUpdateLogOutputStream()) {
            runScript(scripts, out);
        }
    }

    public void runScript(Map<String, String[]> scripts, OutputStream out) {
        String[] script = scripts.get(Common.databaseProxy.getType().name());
        if (script == null)
            script = scripts.get(DEFAULT_DATABASE_TYPE);
        runScript(script, out);
    }

    protected OutputStream createUpdateLogOutputStream() {
        return Common.databaseProxy.createLogOutputStream(this.getClass());
    }

}
