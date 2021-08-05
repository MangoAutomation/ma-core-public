package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade9 extends DBUpgrade {
    private final Logger LOG = LoggerFactory.getLogger(Upgrade9.class);

    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseType.H2.name(), mysqlScript);
        runScript(scripts);

        // Convert existing permissions data.
        // First the data source data
        final Map<Integer, String> dsPermission = new HashMap<>();
        ejt.query("SELECT ds.dataSourceId, u.username FROM dataSourceUsers ds JOIN users u ON ds.userId=u.id",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        int dsId = rs.getInt(1);
                        String username = rs.getString(2);
                        updatePermissionString(dsPermission, dsId, username);
                    }
                });

        for (Entry<Integer, String> e : dsPermission.entrySet())
            ejt.update("UPDATE dataSources SET editPermission=? WHERE id=?", e.getValue(), e.getKey());

        // Now the data point data
        final Map<Integer, String> dpReadPermission = new HashMap<>();
        final Map<Integer, String> dpSetPermission = new HashMap<>();
        ejt.query("SELECT p.dataPointId, p.permission, u.username FROM dataPointUsers p JOIN users u ON p.userId=u.id",
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        int dpId = rs.getInt(1);
                        int permission = rs.getInt(2);
                        String username = rs.getString(3);

                        if (permission == 1) // Read
                            updatePermissionString(dpReadPermission, dpId, username);
                        else if (permission == 2) // Set
                            updatePermissionString(dpSetPermission, dpId, username);
                        else
                            LOG.warn("Unknown permission type in dataPointUsers: " + permission + ", ignored");
                    }
                });

        for (Entry<Integer, String> e : dpReadPermission.entrySet())
            ejt.update("UPDATE dataPoints SET readPermission=? WHERE id=?", e.getValue(), e.getKey());
        for (Entry<Integer, String> e : dpSetPermission.entrySet())
            ejt.update("UPDATE dataPoints SET setPermission=? WHERE id=?", e.getValue(), e.getKey());

        // Goodbye permission tables.
        scripts.put(DatabaseType.DERBY.name(), dropScript);
        scripts.put(DatabaseType.MYSQL.name(), dropScript);
        scripts.put(DatabaseType.MSSQL.name(), dropScript);
        scripts.put(DatabaseType.H2.name(), dropScript);
        runScript(scripts);
    }

    void updatePermissionString(Map<Integer, String> map, int id, String username) {
        String permission = map.get(id);
        if (permission == null)
            permission = "";
        else
            permission += ",";
        permission += username;
        map.put(id, permission);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "10";
    }

    //UPDATE users SET permissions = CONCAT(username, ',user')
    private final String[] mssqlScript = { //
    		"ALTER TABLE users ADD COLUMN permissions NVARCHAR(255);", //
            "ALTER TABLE dataSources ADD COLUMN editPermission NVARCHAR(255);", //
            "ALTER TABLE dataPoints ADD COLUMN readPermission NVARCHAR(255);", //
            "ALTER TABLE dataPoints ADD COLUMN setPermission NVARCHAR(255);", //
            "UPDATE users SET permissions = CONCAT(username, ',user');",
            "UPDATE users SET permissions = CONCAT(permissions, ',superadmin') WHERE admin = 'Y';",
            "ALTER TABLE users DROP COLUMN admin;"
    };

    private final String[] mysqlScript = { //
    		"ALTER TABLE users ADD COLUMN permissions VARCHAR(255);", //
            "ALTER TABLE dataSources ADD COLUMN editPermission VARCHAR(255);", //
            "ALTER TABLE dataPoints ADD COLUMN readPermission VARCHAR(255);", //
            "ALTER TABLE dataPoints ADD COLUMN setPermission VARCHAR(255);", //
            "UPDATE users SET permissions = CONCAT(username, ',user');",
            "UPDATE users SET permissions = CONCAT(permissions, ',superadmin') WHERE admin = 'Y';",
            "ALTER TABLE users DROP COLUMN admin;"
    };

    private final String[] derbyScript = { //
    		"ALTER TABLE users ADD COLUMN permissions VARCHAR(255);", //
            "ALTER TABLE dataSources ADD COLUMN editPermission VARCHAR(255);", //
            "ALTER TABLE dataPoints ADD COLUMN readPermission VARCHAR(255);", //
            "ALTER TABLE dataPoints ADD COLUMN setPermission VARCHAR(255);", //
            "UPDATE users SET permissions = username || ',user';", //Give all users thier own group
            "UPDATE users SET permissions = permissions || ',superadmin' WHERE admin = 'Y';", //Add superadmin to admin users
            "ALTER TABLE users DROP COLUMN admin;"
    };
    
    private final String[] dropScript = { //
    		"DROP TABLE dataSourceUsers;", //
            "DROP TABLE dataPointUsers;", //
    };
}
