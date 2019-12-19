/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.mailingList.MailingList;

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
            Map<String, RoleVO> roles = new HashMap<>();
            convertMailingLists(roles, out);
        } finally {
            out.flush();
            out.close();
        }
    }

    private void convertMailingLists(Map<String, RoleVO> roles, OutputStream out) throws Exception {
        //Move current permissions to roles

        ejt.query("SELECT id, readPermission, editPermission FROM mailingLists", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int voId = rs.getInt(1);
                //Add role/mapping
                String readPermission = rs.getString(2);
                insertMapping(voId, MailingList.class.getSimpleName(), PermissionService.READ, readPermission, roles);
                String editPermission = rs.getString(2);
                insertMapping(voId, MailingList.class.getSimpleName(), PermissionService.EDIT, editPermission, roles);
            }
        });
        
        
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mailingListSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), mailingListSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mailingListMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), mailingListSQL);
        runScript(scripts, out);
    }
    
    private String[] mailingListSQL = new String[]{
            "ALTER TABLE mailingLists DROP COLUMN readPermissions",
            "ALTER TABLE mailingLists DROP COLUMN editPermissions"

    };

    private String[] mailingListMSSQL = new String[]{

    };

    /**
     * Ensure role exists and insert mappings for this permission
     * @param voId
     * @param voType
     * @param permissionType
     * @param existingPermission
     * @param roles
     */
    private void insertMapping(int voId, String voType, String permissionType, String existingPermission, Map<String, RoleVO> roles) {
        
        for(String role : explodePermissionGroups(existingPermission)) {
            roles.compute(role, (k,r) -> {
                if(r == null) {
                    r = new RoleVO();
                    r.setXid(role);
                    r.setName(role);
                    r.setId(ejt.update("INSERT INTO roles (xid, name) values (?,?)", new Object[] {role, role}));
                }
                //Add a mapping
                ejt.update("INSERT INTO roleMappings (roleId, voId, voType, permissionType) VALUES (?,?,?,?)", 
                        new Object[] {
                                r.getId(),
                                voId,
                                voType,
                                permissionType
                        });
                return r;
            });


        }
    }
    
    private Set<String> explodePermissionGroups(String groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> set = new HashSet<>();
        for (String s : groups.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) {
                set.add(s);
            }
        }
        return Collections.unmodifiableSet(set);
    }
    
    @Override
    protected String getNewSchemaVersion() {
        return "30";
    }

}
