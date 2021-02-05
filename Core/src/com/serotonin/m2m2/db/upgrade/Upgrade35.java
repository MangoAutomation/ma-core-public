/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.Collections;

/**
 * Adds the oAuth2Users table
 *
 * @author Jared Wiltshire
 */
public class Upgrade35 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {

        runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                "CREATE TABLE oAuth2Users (id INT NOT NULL AUTO_INCREMENT, issuer VARCHAR(255) NOT NULL, subject VARCHAR(255) NOT NULL, userId INT NOT NULL, PRIMARY KEY (id));",
                "ALTER TABLE oAuth2Users ADD CONSTRAINT oAuth2UsersFk1 FOREIGN KEY (userId) REFERENCES users (id) ON DELETE CASCADE;",
                "ALTER TABLE oAuth2Users ADD CONSTRAINT oAuth2UsersUn1 UNIQUE (issuer, subject);"
        }));
    }

    @Override
    protected String getNewSchemaVersion() {
        return "36";
    }
}
