/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseType;

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

    private final Logger LOG = LoggerFactory.getLogger(Upgrade28.class);

    @Override
    protected void upgrade() throws Exception {

        try (OutputStream out = createUpdateLogOutputStream()) {

            //Update User table to have unique email addresses
            List<UsernameEmail> duplicates = ejt.query("SELECT id,username,email FROM users ORDER BY id asc", rs -> {
                List<UsernameEmail> dupes = new ArrayList<>();
                Set<String> existing = new HashSet<>();

                while (rs.next()) {
                    int id =  rs.getInt(1);
                    String username = rs.getString(2);
                    String email = rs.getString(3);

                    if (!existing.add(email.toLowerCase(Locale.ENGLISH))) {
                        dupes.add(new UsernameEmail(id, username, email));
                    }
                }

                return dupes;
            });

            if (duplicates.size() > 0) {
                duplicates.stream().forEach(u -> {
                    String uniqueEmail = UUID.randomUUID() + u.email;
                    if(uniqueEmail.length() > 255) {
                        uniqueEmail = uniqueEmail.substring(uniqueEmail.length() - 255, uniqueEmail.length());
                    }
                    ejt.update("UPDATE users SET email=? WHERE id=?", new Object[] {uniqueEmail, u.id});
                    LOG.warn("Changing email address for user with duplicate email '" + u.email + "' with id " + u.id + " and username '" + u.username + "' to " + uniqueEmail);
                    PrintWriter pw = new PrintWriter(out);
                    pw.write("WARN: Changing email address for user with duplicate email '" + u.email + "' with id " + u.id + " and username '" + u.username + "' to " + uniqueEmail);
                    pw.flush();
                });
            } else {
                LOG.info("No duplicate email addresses on users, no user email addresses modified.");
            }

            //Create columns for user table
            Map<String, String[]> scripts = new HashMap<>();
            scripts.put(DatabaseType.MYSQL.name(), sqlJSON);
            scripts.put(DatabaseType.H2.name(), sql);
            scripts.put(DatabaseType.MSSQL.name(), mssql);
            scripts.put(DatabaseType.POSTGRES.name(), sqlJSON);
            runScript(scripts, out);

            //Set all the timestamps for the created date to now
            long now = Common.timer.currentTimeMillis();
            scripts.clear();
            String[] setCreatedTs = {"UPDATE users SET createdTs=" + now + ";"};
            scripts.put(DatabaseType.MYSQL.name(), setCreatedTs);
            scripts.put(DatabaseType.H2.name(), setCreatedTs);
            scripts.put(DatabaseType.MSSQL.name(), setCreatedTs);
            scripts.put(DatabaseType.POSTGRES.name(), setCreatedTs);
            runScript(scripts, out);


            //add non null constraint to createdTs
            scripts.clear();
            scripts.put(DatabaseType.MYSQL.name(), mysqlAlterUserCreatedTimestamp);
            scripts.put(DatabaseType.H2.name(), sqlAlterUserCreatedTimestamp);
            scripts.put(DatabaseType.MSSQL.name(), sqlAlterUserCreatedTimestamp);
            scripts.put(DatabaseType.POSTGRES.name(), sqlAlterUserCreatedTimestamp);
            runScript(scripts, out);

            //add unique constraint to email
            scripts.clear();
            scripts.put(DatabaseType.MYSQL.name(), sqlUniqueEmail);
            scripts.put(DatabaseType.H2.name(), sqlUniqueEmail);
            scripts.put(DatabaseType.MSSQL.name(), sqlUniqueEmail);
            scripts.put(DatabaseType.POSTGRES.name(), sqlUniqueEmail);
            runScript(scripts, out);
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

    private static class UsernameEmail {
        final int id;
        final String username;
        final String email;

        public UsernameEmail(int id, String username, String email) {
            this.id = id;
            this.username = username;
            this.email = email;
        }
    }

}
