/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

/**
 * Upgrades the MySQL character set to utf8mb4
 * @author Jared Wiltshire
 */
public class Upgrade30 extends DBUpgrade {
    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DEFAULT_DATABASE_TYPE, new String[0]);

        if (databaseType == DatabaseType.MYSQL) {
            List<String> tableNames = ejt.queryForList("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = database() AND TABLE_TYPE = 'BASE TABLE';", String.class);

            String[] convertCommands = tableNames.stream().map(tableName -> {
                return "ALTER TABLE `" + tableName + "` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
            }).toArray(String[]::new);

            scripts.put(DatabaseType.MYSQL.name(), convertCommands);
        }

        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "31";
    }
}
