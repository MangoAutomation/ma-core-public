/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.BaseDao;

/**
 * Base class for instances that perform database upgrades. The naming of subclasses follows the convention
 * 'Upgrade[version]', where '[version]' is the version that the class upgrades <b>from</b>. The subclass must be in
 * this package.
 *
 * <p>Ensure that you update {@link Common#getDatabaseSchemaVersion()} when implementing upgrades</p>
 *
 * @author Matthew Lohbihler
 */
abstract public class DBUpgrade extends BaseDao {
    protected static final String DEFAULT_DATABASE_TYPE = "*";

    public DBUpgrade() {
        super(null);
    }

    abstract protected void upgrade() throws Exception;

    abstract protected String getNewSchemaVersion();

    /**
     * Convenience method for subclasses
     *
     * @param script the array of script lines to run
     */
    protected void runScript(String[] script) throws IOException {
        try (OutputStream out = createUpdateLogOutputStream()) {
            runScript(script, out);
        }
    }

    protected void runScript(String[] script, OutputStream out) {
        try {
            databaseProxy.runScript(script, out);
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
        String[] script = scripts.get(databaseType.name());
        if (script == null)
            script = scripts.get(DEFAULT_DATABASE_TYPE);
        runScript(script, out);
    }

    protected OutputStream createUpdateLogOutputStream() {
        return databaseProxy.createLogOutputStream(this.getClass());
    }
}
