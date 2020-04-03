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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.Functions;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Fix MySQL data source table to have a name column length of 255
 *
 * Add roles and roleMappings tables
 *
 * MailingList - remove readPermissions and editPermissions
 *
 *
 * Remove User Events table
 *
 * Add JSON Data to data points and data sources
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

        try {

            //Add jsonData column to data points and data sources
            Map<String, String[]> scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), jsonDataColumnsMySQL);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), jsonDataColumnsSql);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), jsonDataColumnsMSSQL);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), jsonDataColumnsMySQL);

            //Update the data source name column length for MySQL
            scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), new String[0]);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), new String[0]);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), alterDataSourceNameColumnMySQL);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), new String[0]);
            runScript(scripts, out);

            //Add data point index for deviceName,name,id
            scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataPointIndexes);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataPointIndexes);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataPointIndexes);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataPointIndexes);
            runScript(scripts, out);

            Map<String, Role> roles = new HashMap<>();
            roles.put(PermissionHolder.SUPERADMIN_ROLE_XID, PermissionHolder.SUPERADMIN_ROLE);
            roles.put(PermissionHolder.USER_ROLE_XID, PermissionHolder.USER_ROLE);

            createRolesTables(roles, out);
            convertUsers(roles, out);
            convertSystemSettingsPermissions(roles, out);
            convertDataPoints(roles, out);
            convertDataSources(roles, out);
            convertJsonData(roles, out);
            convertMailingLists(roles, out);
            convertFileStores(roles, out);
            dropTemplates(out);
            dropPointHierarchy(out);
            dropUserEvents(out);
        } catch(Exception e){
            LOG.error("Upgrade 29 failed.", e);
        } finally {
            out.flush();
            out.close();
        }
    }

    private void dropTemplates(OutputStream out) throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dropTemplatesSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dropTemplatesSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dropTemplatesSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dropTemplatesSQL);
        runScript(scripts, out);
    }

    private void dropPointHierarchy(OutputStream out) throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dropPointHierarchySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dropPointHierarchySQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dropPointHierarchySQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dropPointHierarchySQL);
        runScript(scripts, out);
    }

    private void dropUserEvents(OutputStream out) throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dropUserEventsSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dropUserEventsSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dropUserEventsSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dropUserEventsSQL);
        runScript(scripts, out);
    }

    private void createRolesTables(Map<String, Role> roles, OutputStream out) throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), createRolesMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), createRolesSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), createRolesMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), createRolesSQL);
        runScript(scripts, out);

        //Add default user and superadmin roles
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), defaultRolesSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), defaultRolesSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), defaultRolesSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), defaultRolesSQL);
        runScript(scripts, out);
    }

    private void convertUsers(Map<String, Role> roles, OutputStream out) throws Exception {
        //Move current permissions to roles
        ejt.query("SELECT id, permissions FROM users", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int userId = rs.getInt(1);
                //Get user's current permissions
                Set<String> permissions = explodePermissionGroups(rs.getString(2));
                Set<String> userRoles = new HashSet<>();
                for(String permission : permissions) {
                    //ensure all roles are lower case and don't have spaces on the ends
                    permission = permission.trim();
                    String role = permission.toLowerCase();
                    roles.compute(role, (k,r) -> {
                        if(r == null) {
                            r = new Role(ejt.doInsert("INSERT INTO roles (xid, name) values (?,?)", new Object[] {role, role}), role);
                        }
                        if(!userRoles.contains(role)) {
                            //Add a mapping
                            ejt.doInsert("INSERT INTO userRoleMappings (roleId, userId) VALUES (?,?)",
                                    new Object[] {
                                            r.getId(),
                                            userId
                            });
                            userRoles.add(role);
                        }
                        return r;
                    });
                }
                //Ensure they have the user role
                if(!userRoles.contains(PermissionHolder.USER_ROLE_XID)) {
                    //Add a mapping
                    ejt.doInsert("INSERT INTO userRoleMappings (roleId, userId) VALUES (?,?)",
                            new Object[] {
                                    2,
                                    userId
                    });
                }
            }
        });

        //Drop the permissions column
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), userSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), userSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), userSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), userSQL);
        runScript(scripts, out);
    }

    private void convertSystemSettingsPermissions(Map<String, Role> roles, OutputStream out) throws Exception {
        //Check all permissions
        for (PermissionDefinition def : ModuleRegistry.getDefinitions(PermissionDefinition.class)) {
            //Move to roles and map them
            ejt.query("SELECT settingValue FROM systemSettings WHERE settingName=?", new Object[] {def.getPermissionTypeName()}, new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    //Add role/mapping
                    insertMapping(null, null, def.getPermissionTypeName(), explodePermissionGroups(rs.getString(1)), roles);
                }
            });
            //Delete the setting
            ejt.update("DELETE FROM systemSettings WHERE settingName=?", new Object[] {def.getPermissionTypeName()});
        }
    }

    private void convertDataPoints(Map<String, Role> roles, OutputStream out) throws Exception {
        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, setPermission FROM dataPoints", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int voId = rs.getInt(1);
                //Add role/mapping
                Set<String> readPermissions = explodePermissionGroups(rs.getString(2));
                insertMapping(voId, DataPointVO.class.getSimpleName(), PermissionService.READ, readPermissions, roles);
                Set<String> setPermissions = explodePermissionGroups(rs.getString(3));
                insertMapping(voId, DataPointVO.class.getSimpleName(), PermissionService.SET, setPermissions, roles);
            }
        });


        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataPointsMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataPointsH2SQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataPointsMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataPointsMySQL);
        runScript(scripts, out);
    }

    private void convertDataSources(Map<String, Role> roles, OutputStream out) throws Exception {
        //Move current permissions to roles
        ejt.query("SELECT id, editPermission FROM dataSources", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int voId = rs.getInt(1);
                //Add role/mapping
                Set<String> editPermissions = explodePermissionGroups(rs.getString(2));
                insertMapping(voId, DataSourceVO.class.getSimpleName(), PermissionService.EDIT, editPermissions, roles);
            }
        });


        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataSourcesSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataSourcesSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataSourcesMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataSourcesSQL);
        runScript(scripts, out);
    }

    private void convertJsonData(Map<String, Role> roles, OutputStream out) throws Exception {
        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, editPermission FROM jsonData", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int voId = rs.getInt(1);
                //Add role/mapping
                Set<String> readPermissions = explodePermissionGroups(rs.getString(2));
                insertMapping(voId, JsonDataVO.class.getSimpleName(), PermissionService.READ, readPermissions, roles);
                Set<String> editPermissions = explodePermissionGroups(rs.getString(3));
                insertMapping(voId, JsonDataVO.class.getSimpleName(), PermissionService.EDIT, editPermissions, roles);
            }
        });


        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), jsonDataSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), jsonDataSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), jsonDataSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), jsonDataSQL);
        runScript(scripts, out);
    }

    private void convertMailingLists(Map<String, Role> roles, OutputStream out) throws Exception {
        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, editPermission FROM mailingLists", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int voId = rs.getInt(1);
                //Add role/mapping
                Set<String> readPermissions = explodePermissionGroups(rs.getString(2));
                insertMapping(voId, MailingList.class.getSimpleName(), PermissionService.READ, readPermissions, roles);
                Set<String> editPermissions = explodePermissionGroups(rs.getString(3));
                insertMapping(voId, MailingList.class.getSimpleName(), PermissionService.EDIT, editPermissions, roles);
            }
        });


        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mailingListSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), mailingListSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mailingListSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), mailingListSQL);
        runScript(scripts, out);
    }

    private void convertFileStores(Map<String, Role> roles, OutputStream out) throws Exception {
        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, writePermission FROM fileStores", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int voId = rs.getInt(1);
                //Add role/mapping
                Set<String> readPermissions = explodePermissionGroups(rs.getString(2));
                insertMapping(voId, FileStore.class.getSimpleName(), PermissionService.READ, readPermissions, roles);
                Set<String> writePermissions = explodePermissionGroups(rs.getString(3));
                insertMapping(voId, FileStore.class.getSimpleName(), PermissionService.WRITE, writePermissions, roles);
            }
        });


        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), fileStoreSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), fileStoreSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), fileStoreSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), fileStoreSQL);
        runScript(scripts, out);
    }

    //JSON Data
    private String[] jsonDataColumnsSql = new String[]{
            "ALTER TABLE dataPoints ADD COLUMN jsonData longtext;",
            "ALTER TABLE dataSources ADD COLUMN jsonData longtext;",
            "ALTER TABLE eventDetectors ADD COLUMN jsonData longtext;"
    };
    private String[] jsonDataColumnsMySQL = new String[]{
            "ALTER TABLE dataPoints ADD COLUMN jsonData JSON;",
            "ALTER TABLE dataSources ADD COLUMN jsonData JSON;",
            "ALTER TABLE eventDetectors ADD COLUMN jsonData JSON;"
    };
    private String[] jsonDataColumnsMSSQL = new String[]{
            "ALTER TABLE dataPoints ADD COLUMN jsonData ntext;",
            "ALTER TABLE dataSources ADD COLUMN jsonData ntext;",
            "ALTER TABLE eventDetectors ADD COLUMN jsonData ntext;"
    };

    //Data source fix
    private String[] alterDataSourceNameColumnMySQL = new String[] {
            "ALTER TABLE dataSources MODIFY COLUMN name VARCHAR(255) NOT NULL;"
    };

    //Data point indexes
    private String[] dataPointIndexes = new String[] {
            "CREATE INDEX deviceNameNameIdIndex ON dataPoints (deviceName ASC, name ASC, id ASC);"
    };


    //Templates
    private String[] dropTemplatesSQL = new String[] {
            "DROP TABLE templates;",
    };

    //Roles
    private String[] createRolesSQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id));",
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",

            "CREATE TABLE roleMappings (roleId int not null, voId int, voType varchar(255), permissionType varchar(255) not null);",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);",

            "CREATE TABLE userRoleMappings (roleId int not null, userId int not null);",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);"

    };
    private String[] createRolesMySQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id)) engine=InnoDB;",
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",

            "CREATE TABLE roleMappings (roleId int not null, voId int, voType varchar(255), permissionType varchar(255) not null) engine=InnoDB;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);",

            "CREATE TABLE userRoleMappings (roleId int not null, userId int not null);",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);"
    };
    private String[] createRolesMSSQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id));",
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",

            "CREATE TABLE roleMappings (roleId int not null, voId int, voType nvarchar(255), permissionType varchar(255) not null);",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);",

            "CREATE TABLE userRoleMappings (roleId int not null, userId int not null);",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);"
    };

    //Default role data
    private String[] defaultRolesSQL = new String[] {
            "INSERT INTO roles (id, xid, name) VALUES (1, 'superadmin', 'Superadmin role');",
            "INSERT INTO roles (id, xid, name) VALUES (2, 'user', 'User role');"
    };

    //Point Hierarchy
    private String[] dropPointHierarchySQL = new String[] {
            "DROP TABLE dataPointHierarchy;",
    };

    //User Events
    private String[] dropUserEventsSQL = new String[] {
            "DROP TABLE userEvents;",
    };

    //Users
    private String[] userSQL = new String[] {
            "ALTER TABLE users DROP COLUMN permissions;",
    };

    private String[] dataPointsH2SQL = new String[] {
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP CONSTRAINT dataPointsFk2;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;",
    };

    private String[] dataPointsMySQL = new String[] {
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP FOREIGN KEY dataPointsFk2;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;"
    };

    private String[] dataPointsMSSQL = new String[] {
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP CONSTRAINT dataPointsFk2;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;"
    };


    private String[] dataSourcesSQL = new String[] {
            "ALTER TABLE dataSources DROP COLUMN editPermission;",
    };

    private String[] dataSourcesMSSQL = new String[] {
            "ALTER TABLE dataSources DROP COLUMN editPermission;"
    };

    //Mailing lists
    private String[] mailingListSQL = new String[] {
            "ALTER TABLE mailingLists DROP COLUMN readPermission;",
            "ALTER TABLE mailingLists DROP COLUMN editPermission;"
    };

    private String[] jsonDataSQL = new String[] {
            "ALTER TABLE jsonData DROP COLUMN readPermission;",
            "ALTER TABLE jsonData DROP COLUMN editPermission;"
    };

    private String[] fileStoreSQL = new String[] {
            "ALTER TABLE fileStores DROP COLUMN readPermission;",
            "ALTER TABLE fileStores DROP COLUMN writePermission;"
    };

    /**
     * Get all existing roles so we can ensure we don't create duplicate roles only new mappings
     * @return
     */
    protected Map<String, Role> getExistingRoles() {
        return ejt.query("SELECT id,xid FROM roles", new ResultSetExtractor<Map<String, Role>>() {

            @Override
            public Map<String, Role> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<String, Role> mappings = new HashMap<>();
                while(rs.next()) {
                    int id = rs.getInt(1);
                    String xid = rs.getString(2);
                    mappings.put(xid, new Role(id, xid));
                }
                return mappings;
            }
        });
    }

    /**
     * Ensure role exists and insert mappings for this permission
     *  this is protected for use in modules for this upgrade
     * @param voId
     * @param voType
     * @param permissionType
     * @param existingPermission
     * @param roles
     */
    protected void insertMapping(Integer voId, String voType, String permissionType, Set<String> existingPermissions, Map<String, Role> roles) {
        //Ensure each role is only used 1x for this permission
        Set<String> voRoles = new HashSet<>();
        for(String permission : existingPermissions) {
            //ensure all roles are lower case and don't have spaces on the ends
            permission = permission.trim();
            String role = permission.toLowerCase();
            roles.compute(role, (k,r) -> {
                if(r == null) {
                    r = new Role(ejt.doInsert("INSERT INTO roles (xid, name) values (?,?)", new Object[] {role, role}), role);
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


    /**
     * For use by modules in this upgrade
     * @param groups
     * @return
     */
    protected Set<String> explodePermissionGroups(String groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> set = new HashSet<>();
        for (String s : groups.split(",")) {
            s = s.replaceAll(Functions.WHITESPACE_REGEX, "");
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
