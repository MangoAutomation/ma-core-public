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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.RoleDao;
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

    private RoleVO superadminRole;
    private RoleVO userRole;
    
    @Override
    protected void upgrade() throws Exception {
        OutputStream out = createUpdateLogOutputStream();

        try {
            createRolesTables(out);
            Map<String, RoleVO> roles = new HashMap<>();
            convertMailingLists(roles, out);
        } catch(Exception e){
            LOG.error("Upgrade 29 failed.", e);
        } finally {
            out.flush();
            out.close();
        }
    }

    private void createRolesTables(OutputStream out) throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), createRolesMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), createRolesSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), createRolesMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), createRolesSQL);
        runScript(scripts, out);
        
        //Add default user and superadmin roles
        superadminRole = new RoleVO();
        superadminRole.setXid(RoleDao.SUPERADMIN_ROLE_NAME);
        superadminRole.setName(Common.translate("roles.superadmin"));
        superadminRole.setId(ejt.doInsert("INSERT INTO roles (xid,name) VALUES (?,?)", new Object[] {superadminRole.getXid(), superadminRole.getName()}));
        
        userRole = new RoleVO();
        userRole.setXid(RoleDao.USER_ROLE_NAME);
        userRole.setName(Common.translate("roles.user"));
        userRole.setId(ejt.doInsert("INSERT INTO roles (xid,name) VALUES (?,?)", new Object[] {userRole.getXid(), userRole.getName()}));
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
                String editPermission = rs.getString(3);
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
    
    //Roles
    private String[] createRolesSQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id));", 
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",
            "CREATE TABLE roleMappings (roleId int not null, voId int, voType varchar(255), permissionType varchar(255) not null);",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;" + 
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);"
    };
    private String[] createRolesMySQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id)) engine=InnoDB;", 
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",
            "CREATE TABLE roleMappings (roleId int not null, voId int, voType varchar(255), permissionType varchar(255) not null) engine=InnoDB;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;" + 
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);"
    };
    private String[] createRolesMSSQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id));", 
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",
            "CREATE TABLE roleMappings (roleId int not null, voId int, voType nvarchar(255), permissionType varchar(255) not null);",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;" + 
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);"
    };
    
    //Mailing lists
    private String[] mailingListSQL = new String[] {
            "ALTER TABLE mailingLists DROP COLUMN readPermission;",
            "ALTER TABLE mailingLists DROP COLUMN editPermission;"
    };

    private String[] mailingListMSSQL = new String[]{
            "ALTER TABLE mailingLists DROP COLUMN readPermission;",
            "ALTER TABLE mailingLists DROP COLUMN editPermission;"
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
        //Ensure each role is only used 1x for this permission
        Set<String> voRoles = new HashSet<>();
        for(String permission : explodePermissionGroups(existingPermission)) {
            //ensure all roles are lower case and don't have spaces on the ends
            permission = permission.trim();
            String role = permission.toLowerCase();
            roles.compute(role, (k,r) -> {
                if(r == null) {
                    if(StringUtils.equalsIgnoreCase(role, RoleDao.SUPERADMIN_ROLE_NAME)) {
                        r = superadminRole;
                    }else if(StringUtils.equalsIgnoreCase(role, RoleDao.USER_ROLE_NAME)) {
                        r = userRole;
                    }else {
                        r = new RoleVO();
                        r.setXid(role);
                        r.setName(role);
                        r.setId(ejt.doInsert("INSERT INTO roles (xid, name) values (?,?)", new Object[] {role, role}));
                    }
                }
                if(!voRoles.contains(role)) {
                    //Add a mapping
                    ejt.doInsert("INSERT INTO roleMappings (roleId, voId, voType, permissionType) VALUES (?,?,?,?)", 
                            new Object[] {
                                    r.getId(),
                                    voId,
                                    voType,
                                    permissionType
                            });
                    voRoles.add(role);
                }
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
