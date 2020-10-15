/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.transaction.support.TransactionTemplate;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Adds readPermissionId column to events table
 * @author Terry Packer
 */
public class Upgrade33 extends DBUpgrade implements PermissionMigration {

    private Map<Integer, Integer> dataPointPermissionMap = new HashMap<>();
    private Map<Integer, Integer> dataSourcePermissionMap = new HashMap<>();
    private final Map<MangoPermission, MangoPermission> permissionCache = new HashMap<>();
    private final Map<Role, Role> roleCache = new HashMap<>();

    @Override
    protected void upgrade() throws Exception {

        try (OutputStream out = createUpdateLogOutputStream()) {
            //Get a reference to the superadmin permission in DB
            MangoPermission superadmin = getOrCreatePermissionNoCache(MangoPermission.superadminOnly());

            //Add readPermission ID column to events
            runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                    "ALTER TABLE events ADD COLUMN readPermissionId INT;",
                    "ALTER TABLE events ADD CONSTRAINT eventsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            }), out);

            //Upgrade all permissions
            ejt.query("SELECT id, typeName, typeRef1 FROM events", rs -> {
                int eventId = rs.getInt(1);
                String typeName = rs.getString(2);
                Integer voId = rs.getInt(3);
                Integer readPermissionId = null;
                if (typeName.equals(EventType.EventTypeNames.DATA_POINT)) {
                    readPermissionId = dataPointPermissionMap.computeIfAbsent(voId, (k) -> {
                        Integer id = ejt.queryForInt("SELECT readPermissionId from dataPoints where id=?", new Object[] {k}, Common.NEW_ID);
                        if(id == Common.NEW_ID) {
                            return superadmin.getId();
                        }else {
                            return id;
                        }
                    });
                }else if (typeName.equals(EventType.EventTypeNames.DATA_SOURCE)) {
                    readPermissionId = dataSourcePermissionMap.computeIfAbsent(voId, (k) -> {
                        Integer id = ejt.queryForInt("SELECT readPermissionId from dataSources where id=?", new Object[] {k}, Common.NEW_ID);
                        if(id == Common.NEW_ID) {
                            return superadmin.getId();
                        }else {
                            return id;
                        }
                    });

                }else if (typeName.equals(EventType.EventTypeNames.SYSTEM)) {
                    readPermissionId = superadmin.getId();
                }else if (typeName.equals(EventType.EventTypeNames.PUBLISHER)) {
                    readPermissionId = superadmin.getId();
                }else if (typeName.equals(EventType.EventTypeNames.AUDIT)) {
                    readPermissionId = superadmin.getId();
                }else {
                    //Let the module upgrades do this
                    readPermissionId = superadmin.getId();
                }

                if(readPermissionId == null || readPermissionId == Common.NEW_ID) {
                    readPermissionId = superadmin.getId();
                }

                ejt.update("UPDATE events SET readPermissionId=? WHERE id=?", readPermissionId, eventId);
            });

            //Make NON-NULL
            HashMap<String, String[]> scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), new String[] {"ALTER TABLE events MODIFY COLUMN readPermissionId INT NOT NULL;"});
            scripts.put(DEFAULT_DATABASE_TYPE,  new String[] {"ALTER TABLE events ALTER COLUMN readPermissionId INT NOT NULL;"});
            runScript(scripts, out);
        }
    }

    @Override
    protected String getNewSchemaVersion() {
        return "34";
    }

    @Override
    public ExtendedJdbcTemplate getJdbcTemplate() {
        return ejt;
    }

    @Override
    public Map<MangoPermission, MangoPermission> permissionCache() {
        return permissionCache;
    }

    @Override
    public Map<Role, Role> roleCache() {
        return roleCache;
    }

    @Override
    public TransactionTemplate getTransactionTemplate() {
        return super.getTransactionTemplate();
    }
}
