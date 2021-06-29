/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

/**
 * @author Jared Wiltshire
 */
public class Upgrade20 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.MYSQL.name(), mysql);
        scripts.put(DatabaseType.H2.name(), h2);
        scripts.put(DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseType.POSTGRES.name(), postgres);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "21";
    }
    
    private final String[] mysql = new String[] {
        "CREATE TABLE dataPointTags (" +
        "  dataPointId INT NOT NULL," +
        "  tagKey VARCHAR(255) NOT NULL," +
        "  tagValue VARCHAR(255) NOT NULL" +
        ") engine=InnoDB;",
        "ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsUn1 UNIQUE (dataPointId ASC, tagKey ASC);",
        "ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;",
        "CREATE INDEX dataPointTagsIndex1 ON dataPointTags (tagKey ASC, tagValue ASC);",
        "INSERT INTO dataPointTags (dataPointId, tagKey, tagValue) SELECT id, 'name', name FROM dataPoints WHERE name IS NOT NULL AND name <> '';",
        "INSERT INTO dataPointTags (dataPointId, tagKey, tagValue) SELECT id, 'device', deviceName FROM dataPoints WHERE deviceName IS NOT NULL AND deviceName <> '';",
        
        // create some new indexes on other dataPoints and dataSources
        "CREATE INDEX dataPointsPermissionIndex on dataPoints (dataSourceId ASC, readPermission ASC, setPermission ASC);",
        "CREATE INDEX dataSourcesPermissionIndex on dataSources (editPermission ASC);"
    };
    
    private final String[] h2 = new String[] {
        "CREATE TABLE dataPointTags (" +
        "  dataPointId INT NOT NULL," +
        "  tagKey VARCHAR(255) NOT NULL," +
        "  tagValue VARCHAR(255) NOT NULL" +
        ");",
        "ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsUn1 UNIQUE (dataPointId ASC, tagKey ASC);",
        "ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;",
        "CREATE INDEX dataPointTagsIndex1 ON dataPointTags (tagKey ASC, tagValue ASC);",
        "INSERT INTO dataPointTags (dataPointId, tagKey, tagValue) SELECT id, 'name', name FROM dataPoints WHERE name IS NOT NULL AND name <> '';",
        "INSERT INTO dataPointTags (dataPointId, tagKey, tagValue) SELECT id, 'device', deviceName FROM dataPoints WHERE deviceName IS NOT NULL AND deviceName <> '';",
        
        // create some new indexes on other dataPoints and dataSources
        "CREATE INDEX dataPointsPermissionIndex on dataPoints (dataSourceId ASC, readPermission ASC, setPermission ASC);",
        "CREATE INDEX dataSourcesPermissionIndex on dataSources (editPermission ASC);"
    };
    
    private final String[] mssql = new String[] {
        "CREATE TABLE dataPointTags (" +
        "  dataPointId INT NOT NULL," +
        "  tagKey VARCHAR(255) NOT NULL," +
        "  tagValue VARCHAR(255) NOT NULL," +
        "  PRIMARY KEY (dataPointId, tagKey)" +
        ");",
        "ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;",
        "ALTER TABLE dataPointTags ADD INDEX dataPointTagsIndex1 (tagKey ASC, tagValue ASC);",
        "INSERT INTO dataPointTags (dataPointId, tagKey, tagValue) SELECT id, 'name', name FROM dataPoints WHERE name IS NOT NULL AND name <> '';",
        "INSERT INTO dataPointTags (dataPointId, tagKey, tagValue) SELECT id, 'device', deviceName FROM dataPoints WHERE deviceName IS NOT NULL AND deviceName <> '';",
        
        // create some new indexes on other dataPoints and dataSources
        "CREATE INDEX dataPointsPermissionIndex on dataPoints (dataSourceId ASC, readPermission ASC, setPermission ASC);",
        "CREATE INDEX dataSourcesPermissionIndex on dataSources (editPermission ASC);"
    };
    
    private final String[] postgres = new String[] {
        "CREATE TABLE dataPointTags (" +
        "  dataPointId INT NOT NULL," +
        "  tagKey VARCHAR(255) NOT NULL," +
        "  tagValue VARCHAR(255) NOT NULL," +
        "  PRIMARY KEY (dataPointId, tagKey)" +
        ");",
        "ALTER TABLE dataPointTags ADD CONSTRAINT dataPointTagsFk1 FOREIGN KEY (dataPointId) REFERENCES dataPoints (id) ON DELETE CASCADE;",
        "ALTER TABLE dataPointTags ADD INDEX dataPointTagsIndex1 (tagKey ASC, tagValue ASC);",
        "INSERT INTO dataPointTags (dataPointId, tagKey, tagValue) SELECT id, 'name', name FROM dataPoints WHERE name IS NOT NULL AND name <> '';",
        "INSERT INTO dataPointTags (dataPointId, tagKey, tagValue) SELECT id, 'device', deviceName FROM dataPoints WHERE deviceName IS NOT NULL AND deviceName <> '';",
        
        // create some new indexes on other dataPoints and dataSources
        "CREATE INDEX dataPointsPermissionIndex on dataPoints (dataSourceId ASC, readPermission ASC, setPermission ASC);",
        "CREATE INDEX dataSourcesPermissionIndex on dataSources (editPermission ASC);"
    };
}
