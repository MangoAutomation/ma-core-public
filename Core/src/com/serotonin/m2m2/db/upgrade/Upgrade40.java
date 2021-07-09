/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import org.jooq.SQLDialect;

import com.infiniteautomation.mango.db.tables.Users;

/**
 * Fix locale column having a non-null constraint from {@link Upgrade13}
 *
 * @author Jared Wiltshire
 */
public class Upgrade40 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        Users users = Users.USERS;

        // set back to nullable
        if (create.configuration().dialect() == SQLDialect.H2) {
            create.alterTable(users)
                    .alterColumn(users.locale)
                    .dropNotNull()
                    .execute();
        } else {
            create.alterTable(users)
                    .alterColumn(users.locale)
                    .set(users.locale.getDataType())
                    .execute();
        }
    }

    @Override
    protected String getNewSchemaVersion() {
        return "41";
    }
}
