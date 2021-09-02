/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import org.jooq.SQLDialect;

/**
 * Convert MySQL tables to all use innoDB engine
 *
 * @author Terry Packer
 */
public class Upgrade41 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        if (create.configuration().dialect() == SQLDialect.MYSQL) {
            create.execute("ALTER TABLE roleInheritance ENGINE=InnoDB");
            create.execute("ALTER TABLE oAuth2Users ENGINE=InnoDB");
        }
    }

    @Override
    protected String getNewSchemaVersion() {
        return "42";
    }
}
