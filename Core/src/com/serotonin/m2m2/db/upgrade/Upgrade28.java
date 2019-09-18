/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.UserDao;

/**
 * Add organization column to users table
 * Add organizationalRole column to users table
 * Add createdTs column to users table
 * Add emailVerified column to users table
 * Add data column to users table
 * Make email addresses unique
 * Make createdTs column NOT NULL
 * 
 * 
 * @author Terry Packer
 *
 */
public class Upgrade28 extends DBUpgrade {
    
    private final Log LOG = LogFactory.getLog(Upgrade28.class);
    
    @Override
    protected void upgrade() throws Exception {
        OutputStream out = createUpdateLogOutputStream();
        //Update User table to have unique email addresses
        //First remove duplicate users
        try {
            Map<Integer, StringStringPair> toRemove = query("SELECT id,username,email FROM users ORDER BY id asc", new ResultSetExtractor<Map<Integer,StringStringPair>>(){
    
                @Override
                public Map<Integer, StringStringPair> extractData(ResultSet rs)
                        throws SQLException, DataAccessException {
                    Map<Integer, StringStringPair> remove = new HashMap<>();
                    Map<String, Integer> existing = new HashMap<>();
                    while(rs.next()) {
                        if(null != existing.put(rs.getString(3), rs.getInt(1))) {
                            remove.put(rs.getInt(1), new StringStringPair(rs.getString(2), rs.getString(3)));
                        }
                    }
                    return remove;
                }
                
            });
            
            if(toRemove.keySet().size() > 0) {
                toRemove.keySet().stream().forEach((key) -> {
                    String username = toRemove.get(key).getKey();
                    String email = toRemove.get(key).getValue();
                    LOG.warn("Removing user with duplicate email '" + email + "' with id " + key + " and username '" + username + "'");
                    UserDao.getInstance().deleteUser(key);
                    PrintWriter pw = new PrintWriter(out);
                    pw.write("WARN: Removing user with duplicate email '" + email + "' with id " + key + " and username '" + username + "'\n");
                    pw.flush();
                });
            } else {
                LOG.info("No duplicate email addresses on users, no users removed.");
            }
            
            //Create columns for user table
            Map<String, String[]> scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), sqlJSON);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), sql);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sqlJSON);
            runScript(scripts, out);
            
            //Set all the timestamps for the created date to now
            long now = Common.timer.currentTimeMillis();
            scripts.clear();
            String[] setCreatedTs = {"UPDATE users SET createdTs=" + now + ";"};
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), setCreatedTs);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), setCreatedTs);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), setCreatedTs);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), setCreatedTs);
            runScript(scripts, out);

            
            //add non null constraint to createdTs
            scripts.clear();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlAlterUserCreatedTimestamp);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), sqlAlterUserCreatedTimestamp);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), sqlAlterUserCreatedTimestamp);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sqlAlterUserCreatedTimestamp);
            runScript(scripts, out);
            
            //add unique constraint to email
            scripts.clear();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), sqlUniqueEmail);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), sqlUniqueEmail);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), sqlUniqueEmail);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sqlUniqueEmail);
            runScript(scripts, out);
        } finally {
            out.flush();
            out.close();
        }
    }
    
    private String[] sql = new String[]{
            "ALTER TABLE users ADD COLUMN organization varchar(80);",
            "ALTER TABLE users ADD COLUMN organizationalRole varchar(80);",
            "ALTER TABLE users ADD COLUMN createdTs bigint;",
            "ALTER TABLE users ADD COLUMN emailVerifiedTs bigint;",
            "ALTER TABLE users ADD COLUMN data longtext;",
    };
    private String[] sqlJSON = new String[]{
            "ALTER TABLE users ADD COLUMN organization varchar(80);",
            "ALTER TABLE users ADD COLUMN organizationalRole varchar(80);",
            "ALTER TABLE users ADD COLUMN createdTs bigint;",
            "ALTER TABLE users ADD COLUMN emailVerifiedTs bigint;",
            "ALTER TABLE users ADD COLUMN data JSON;",
    };
    private String[] mssql = new String[]{
            "ALTER TABLE users ADD COLUMN organization nvarchar(80);",
            "ALTER TABLE users ADD COLUMN organizationalRole nvarchar(80);",
            "ALTER TABLE users ADD COLUMN createdTs bigint;",
            "ALTER TABLE users ADD COLUMN emailVerifiedTs bigint;",
            "ALTER TABLE users ADD COLUMN data ntext;",
    };
    
    private String[] mysqlAlterUserCreatedTimestamp = new String[] {
            "ALTER TABLE users MODIFY COLUMN createdTs BIGINT NOT NULL;"
    };
    private String[] sqlAlterUserCreatedTimestamp = new String[] {
            "ALTER TABLE users ALTER COLUMN createdTs BIGINT NOT NULL;"
    };

    private String[] sqlUniqueEmail = new String[] {
            "ALTER TABLE users ADD CONSTRAINT email_unique UNIQUE(email);"
    };
    
    @Override
    protected String getNewSchemaVersion() {
        return "29";
    }
}
