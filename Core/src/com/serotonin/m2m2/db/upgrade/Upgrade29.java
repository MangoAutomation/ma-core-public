/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * Add roles and roleMappings tables
 * 
 * MailingList - remove readPermissions and editPermissions
 *
 *
 * @author Terry Packer
 *
 */
public class Upgrade29 extends DBUpgrade {

    private final Log LOG = LogFactory.getLog(Upgrade29.class);

    @Override
    protected void upgrade() throws Exception {
        OutputStream out = createUpdateLogOutputStream();
        
        //TODO Add roles table and mapping table create statements
        
        try {

            Map<String, String[]> scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), sql);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), sql);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sql);
            runScript(scripts, out);

        } finally {
            out.flush();
            out.close();
        }
    }

    private String[] sql = new String[]{
            "ALTER TABLE mailingLists DROP COLUMN readPermissions",
            "ALTER TABLE mailingLists DROP COLUMN editPermissions"

    };

    private String[] mssql = new String[]{

    };

    @Override
    protected String getNewSchemaVersion() {
        return "30";
    }

}
