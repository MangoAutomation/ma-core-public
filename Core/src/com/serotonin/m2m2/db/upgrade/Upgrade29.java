/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.DSLContext;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.FileStore;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.util.SerializationHelper;

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
public class Upgrade29 extends DBUpgrade implements PermissionMigration {

    private final Log LOG = LogFactory.getLog(Upgrade29.class);

    @Override
    protected void upgrade() throws Exception {
        OutputStream out = createUpdateLogOutputStream();

        try {

            //Add session data table for Jetty session persistence
            Map<String, String[]> scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), sessionDataMySQL);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), sessionDataSQL);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), sessionDataMSSQL);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sessionDataSQL);
            runScript(scripts, out);

            //Add jsonData column to data points and data sources
            scripts = new HashMap<>();
            scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), jsonDataColumnsMySQL);
            scripts.put(DatabaseProxy.DatabaseType.H2.name(), jsonDataColumnsSql);
            scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), jsonDataColumnsMSSQL);
            scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), jsonDataColumnsMySQL);
            runScript(scripts, out);

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
            //TODO Mango 4.0 Fix up
            //convertEmailEventHandlers(out);
            //convertSetPointEventHandlers(out);
            dropTemplates(out);
            dropPointHierarchy(out);
            dropUserEvents(out);

            //delete work directory
            try {
                Path workPath = Common.MA_HOME_PATH.resolve("work");
                FileSystemUtils.deleteRecursively(workPath);
                PrintWriter pw = new PrintWriter(out);
                pw.write("Deleted " + workPath);
                pw.flush();
            }catch(Exception e) {
                LOG.warn("Failed to delete work directory at " + Common.MA_HOME_PATH.resolve("work"), e);
            }

        } catch(Exception e){
            LOG.error("Upgrade 29 failed.", e);
            throw e;
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
        //Create permission columns
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataPointPermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataPointPermissionH2SQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataPointPermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataPointPermissionMySQL);
        runScript(scripts, out);

        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, setPermission FROM dataPoints", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int voId = rs.getInt(1);
                //Add role/mapping
                Set<String> readPermissions = explodePermissionGroups(rs.getString(2));
                Integer read = insertMapping(voId, DataPointVO.class.getSimpleName(), PermissionService.READ, readPermissions, roles);
                Set<String> setPermissions = explodePermissionGroups(rs.getString(3));
                Integer set = insertMapping(voId, DataPointVO.class.getSimpleName(), PermissionService.SET, setPermissions, roles);
                ejt.update("UPDATE dataPoints SET readPermissionId=?,setPermissionId=? WHERE id=?", new Object[] {read, set, voId});
            }
        });


        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataPointsMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataPointsH2SQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataPointsMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataPointsMySQL);
        runScript(scripts, out);
    }

    private void convertDataSources(Map<String, Role> roles, OutputStream out) throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataSourcePermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataSourcePermissionH2SQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataSourcePermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataSourcePermissionMySQL);
        runScript(scripts, out);

        //Move current permissions to roles
        ejt.query("SELECT id, editPermission FROM dataSources", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int voId = rs.getInt(1);
                //Add role/mapping
                Set<String> editPermissions = explodePermissionGroups(rs.getString(2));
                Integer edit = insertMapping(voId, DataSourceVO.class.getSimpleName(), PermissionService.EDIT, editPermissions, roles);
                ejt.update("UPDATE dataSources SET editPermissionId=? WHERE id=?", new Object[] {edit, voId});
            }
        });


        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataSourcesMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataSourcesH2SQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataSourcesMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataSourcesMySQL);
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

    /**
     * Read out data and re-serialize to convert script roles
     */
    private void convertEmailEventHandlers(OutputStream out) {
        //Read and save all persistent data sources to bump their serialization version
        this.ejt.query("SELECT id, data FROM eventHandlers eh WHERE eh.eventHandlerType=?", new Object[] {EmailEventHandlerDefinition.TYPE_NAME}, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int id = rs.getInt(1);
                AbstractEventHandlerVO vo = (AbstractEventHandlerVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(2));
                ejt.update("UPDATE eventHandlers SET data=? where id=?",
                        new Object[] {SerializationHelper.writeObjectToArray(vo), id});
            }
        });
    }

    /**
     * Read out data and re-serialize to convert script roles
     * @param out
     */
    private void convertSetPointEventHandlers(OutputStream out) {
        //Read and save all persistent data sources to bump their serialization version
        this.ejt.query("SELECT id, data FROM eventHandlers eh WHERE eh.eventHandlerType=?", new Object[] {SetPointEventHandlerDefinition.TYPE_NAME}, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int id = rs.getInt(1);
                AbstractEventHandlerVO vo = (AbstractEventHandlerVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(2));
                ejt.update("UPDATE eventHandlers SET data=? where id=?",
                        new Object[] {SerializationHelper.writeObjectToArray(vo), id});
            }
        });
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

            "CREATE TABLE roleMappings (roleId int not null, voId int, voType varchar(255), permissionType varchar(255) not null, mask BIGINT NOT NULL);",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);",
            "CREATE INDEX roleMappingsPermissionTypeIndex ON roleMappings (permissionType ASC);",
            "CREATE INDEX roleMappingsVoTypeIndex ON roleMappings (voType ASC);",
            "CREATE INDEX roleMappingsVoIdIndex ON roleMappings (voId ASC);",
            "CREATE INDEX roleMappingsRoleIdIndex ON roleMappings (roleId ASC);",
            "CREATE INDEX roleMappingsVoTypeVoIdPermissionTypeIndex ON roleMappings (voType ASC, voId ASC, permissionType ASC);",

            "CREATE TABLE userRoleMappings (roleId int not null, userId int not null);",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);",

            "CREATE TABLE roleInheritance (roleId INT NOT NULL, inheritedRoleId INT NOT NULL);",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceUn1 UNIQUE (roleId,inheritedRoleId);",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk2 FOREIGN KEY (inheritedRoleId) REFERENCES roles(id) ON DELETE CASCADE;",

            "CREATE TABLE minterms (id int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (id));",

            "CREATE TABLE mintermsRoles (mintermId int(11) NOT NULL, roleId int(11) NOT NULL);",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesIdx1 UNIQUE (mintermId,roleId);",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk1Idx KEY (mintermId);",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk2_idx KEY (roleId);",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk1 FOREIGN KEY (mintermId) REFERENCES minterms (id) ON DELETE CASCADE ON UPDATE NO ACTION;",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk2 FOREIGN KEY (roleId) REFERENCES roles (id) ON DELETE CASCADE ON UPDATE NO ACTION;",

            "CREATE TABLE permissions (id int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (id));",

            "CREATE TABLE permissionsMinterms (permissionId int(11) NOT NULL, mintermId int(11) NOT NULL);",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsIdx1 UNIQUE KEY (permissionId, mintermId);",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk1Idx KEY (permissionId);",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk2Idx KEY(mintermId);",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk1 FOREIGN KEY (permissionId) REFERENCES permissions (id) ON DELETE CASCADE ON UPDATE NO ACTION;",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk2 FOREIGN KEY (mintermId) REFERENCES minterms (id) ON DELETE CASCADE ON UPDATE NO ACTION;"
    };
    private String[] createRolesMySQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id)) engine=InnoDB;",
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",

            "CREATE TABLE roleMappings (roleId int not null, voId int, voType varchar(255), permissionType varchar(255) not null, mask BIGINT NOT NULL) engine=InnoDB;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);",
            "CREATE INDEX roleMappingsPermissionTypeIndex ON roleMappings (permissionType ASC);",
            "CREATE INDEX roleMappingsVoTypeIndex ON roleMappings (voType ASC);",
            "CREATE INDEX roleMappingsVoIdIndex ON roleMappings (voId ASC);",
            "CREATE INDEX roleMappingsRoleIdIndex ON roleMappings (roleId ASC);",
            "CREATE INDEX roleMappingsVoTypeVoIdPermissionTypeIndex ON roleMappings (voType ASC, voId ASC, permissionType ASC);",

            "CREATE TABLE userRoleMappings (roleId int not null, userId int not null) engine=InnoDB;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);",

            "CREATE TABLE roleInheritance (roleId INT NOT NULL, inheritedRoleId INT NOT NULL) engine=InnoDB;",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceUn1 UNIQUE (roleId,inheritedRoleId);",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk2 FOREIGN KEY (inheritedRoleId) REFERENCES roles(id) ON DELETE CASCADE;",

            "CREATE TABLE minterms (id int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (id)) ENGINE=InnoDB;",

            "CREATE TABLE mintermsRoles (",
            "mintermId int(11) NOT NULL,",
            "roleId int(11) NOT NULL,",
            "UNIQUE KEY mintermsRolesIdx1 (mintermId, roleId),",
            "KEY mintermsRolesFk1Idx (mintermId),",
            "KEY mintermsRolesFk2Idx (roleId),",
            "CONSTRAINT mintermsRolesFk1 FOREIGN KEY (mintermId) REFERENCES minterms (id) ON DELETE CASCADE ON UPDATE NO ACTION,",
            "CONSTRAINT mintermsRolesFk2 FOREIGN KEY (roleId) REFERENCES roles (id) ON DELETE CASCADE ON UPDATE NO ACTION",
            ") ENGINE=InnoDB;",

            "CREATE TABLE permissions (",
            "id int(11) NOT NULL AUTO_INCREMENT,",
            "PRIMARY KEY (id)",
            ") ENGINE=InnoDB;",


            "CREATE TABLE permissionsMinterms (",
            "permissionId int(11) NOT NULL,",
            "mintermId int(11) NOT NULL,",
            "UNIQUE KEY permissionsMintermsIdx1 (permissionId, mintermId),",
            "KEY permissionsMintermsFk1Idx (permissionId),",
            "KEY permissionsMintermsFk2Idx (mintermId),",
            "CONSTRAINT permissionsMintermsFk1 FOREIGN KEY (permissionId) REFERENCES permissions (id) ON DELETE CASCADE ON UPDATE NO ACTION,",
            "CONSTRAINT permissionsMintermsFk2 FOREIGN KEY (mintermId) REFERENCES minterms (id) ON DELETE CASCADE ON UPDATE NO ACTION",
            ") ENGINE=InnoDB;"

    };
    private String[] createRolesMSSQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id));",
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",

            "CREATE TABLE roleMappings (roleId int not null, voId int, voType nvarchar(255), permissionType varchar(255) not null, mask BIGINT NOT NULL);",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleMappings ADD CONSTRAINT roleMappingsUn1 UNIQUE (roleId,voId,voType,permissionType);",
            "CREATE INDEX roleMappingsPermissionTypeIndex ON roleMappings (permissionType ASC);",
            "CREATE INDEX roleMappingsVoTypeIndex ON roleMappings (voType ASC);",
            "CREATE INDEX roleMappingsVoIdIndex ON roleMappings (voId ASC);",
            "CREATE INDEX roleMappingsRoleIdIndex ON roleMappings (roleId ASC);",
            "CREATE INDEX roleMappingsVoTypeVoIdPermissionTypeIndex ON roleMappings (voType ASC, voId ASC, permissionType ASC);",

            "CREATE TABLE userRoleMappings (roleId int not null, userId int not null);",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsFk2 FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE;",
            "ALTER TABLE userRoleMappings ADD CONSTRAINT userRoleMappingsUn1 UNIQUE (roleId,userId);",

            "CREATE TABLE roleInheritance (roleId INT NOT NULL, inheritedRoleId INT NOT NULL);",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceUn1 UNIQUE (roleId,inheritedRoleId);",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk1 FOREIGN KEY (roleId) REFERENCES roles(id) ON DELETE CASCADE;",
            "ALTER TABLE roleInheritance ADD CONSTRAINT roleInheritanceFk2 FOREIGN KEY (inheritedRoleId) REFERENCES roles(id) ON DELETE CASCADE;",

            "CREATE TABLE minterms (id int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (id));",

            "CREATE TABLE mintermsRoles (mintermId int(11) NOT NULL, roleId int(11) NOT NULL);",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesIdx1 UNIQUE (mintermId,roleId);",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk1Idx KEY (mintermId);",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk2_idx KEY (roleId);",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk1 FOREIGN KEY (mintermId) REFERENCES minterms (id) ON DELETE CASCADE ON UPDATE NO ACTION;",
            "ALTER TABLE mintermsRoles ADD CONSTRAINT mintermsRolesFk2 FOREIGN KEY (roleId) REFERENCES roles (id) ON DELETE CASCADE ON UPDATE NO ACTION;",

            "CREATE TABLE permissions (id int(11) NOT NULL AUTO_INCREMENT, PRIMARY KEY (id));",

            "CREATE TABLE permissionsMinterms (permissionId int(11) NOT NULL, mintermId int(11) NOT NULL);",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsIdx1 UNIQUE KEY (permissionId, mintermId);",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk1Idx KEY (permissionId);",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk2Idx KEY(mintermId);",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk1 FOREIGN KEY (permissionId) REFERENCES permissions (id) ON DELETE CASCADE ON UPDATE NO ACTION;",
            "ALTER TABLE permissionsMinterms ADD CONSTRAINT permissionsMintermsFk2 FOREIGN KEY (mintermId) REFERENCES minterms (id) ON DELETE CASCADE ON UPDATE NO ACTION;"

    };

    //Default role data
    private String[] defaultRolesSQL = new String[] {
            "INSERT INTO roles (id, xid, name) VALUES (1, 'superadmin', 'Superadmin role');",
            "INSERT INTO roles (id, xid, name) VALUES (2, 'user', 'User role');",
            "INSERT INTO roles (id, xid, name) VALUES (3, 'anonymous', 'Anonymous role');"
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
            "DROP INDEX dataPointsPermissionIndex;",
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;",
    };
    private String[] dataPointPermissionH2SQL = new String[] {
            "ALTER TABLE dataPoints DROP CONSTRAINT dataPointsFk2;",
            "ALTER TABLE dataPoints ADD COLUMN readPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataPoints ADD COLUMN setPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk3 FOREIGN KEY (setPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;"
    };

    private String[] dataPointsMySQL = new String[] {
            "ALTER TABLE dataPoints DROP INDEX dataPointsPermissionIndex;",
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;",
    };
    private String[] dataPointPermissionMySQL = new String[] {
            "ALTER TABLE dataPoints DROP FOREIGN KEY dataPointsFk2;",
            "ALTER TABLE dataPoints ADD COLUMN readPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataPoints ADD COLUMN setPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk3 FOREIGN KEY (setPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;"
    };

    private String[] dataPointsMSSQL = new String[] {
            "DROP INDEX dataPoints.dataPointsPermissionIndex;",
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;",
    };
    private String[] dataPointPermissionMSSQL = new String[] {
            "ALTER TABLE dataPoints DROP CONSTRAINT dataPointsFk2;",
            "ALTER TABLE dataPoints ADD COLUMN readPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataPoints ADD COLUMN setPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk3 FOREIGN KEY (setPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;"

    };

    private String[] dataSourcesH2SQL = new String[] {
            "DROP INDEX dataSourcesPermissionIndex;",
            "ALTER TABLE dataSources DROP COLUMN editPermission;",
    };
    private String[] dataSourcePermissionH2SQL = new String[] {
            "ALTER TABLE dataSources ADD COLUMN readPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataSources ADD COLUMN editPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;"
    };

    private String[] dataSourcesMySQL = new String[] {
            "ALTER TABLE dataPoints DROP INDEX dataSourcesPermissionIndex;",
            "ALTER TABLE dataSources DROP COLUMN editPermission;",
    };
    private String[] dataSourcePermissionMySQL = new String[] {
            "ALTER TABLE dataSources ADD COLUMN readPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataSources ADD COLUMN editPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;"
    };

    private String[] dataSourcesMSSQL = new String[] {
            "DROP INDEX dataSources.dataSourcesPermissionIndex;",
            "ALTER TABLE dataSources DROP COLUMN editPermission;",
    };

    private String[] dataSourcePermissionMSSQL = new String[] {
            "ALTER TABLE dataSources ADD COLUMN readPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataSources ADD COLUMN editPermissionId INT DEFAULT NULL;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE SET NULL;"
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

    private String [] sessionDataMySQL = new String[] {
            "CREATE TABLE mangoSessionData (sessionId VARCHAR(120), contextPath VARCHAR(60), virtualHost VARCHAR(60), lastNode VARCHAR(60), accessTime BIGINT, lastAccessTime BIGINT, createTime BIGINT, cookieTime BIGINT, lastSavedTime BIGINT, expiryTime BIGINT, maxInterval BIGINT, userId INT, primary key (sessionId, contextPath, virtualHost))engine=InnoDB;",
            "CREATE INDEX mangoSessionDataExpiryIndex ON mangoSessionData (expiryTime);",
            "CREATE INDEX mangoSessionDataSessionIndex ON mangoSessionData (sessionId, contextPath);"
    };

    private String[] sessionDataSQL = new String[] {
            "CREATE TABLE mangoSessionData (sessionId VARCHAR(120), contextPath VARCHAR(60), virtualHost VARCHAR(60), lastNode VARCHAR(60), accessTime BIGINT, lastAccessTime BIGINT, createTime BIGINT, cookieTime BIGINT, lastSavedTime BIGINT, expiryTime BIGINT, maxInterval BIGINT, userId INT, primary key (sessionId, contextPath, virtualHost));",
            "CREATE INDEX mangoSessionDataExpiryIndex ON mangoSessionData (expiryTime);",
            "CREATE INDEX mangoSessionDataSessionIndex ON mangoSessionData (sessionId, contextPath);"
    };

    private String[] sessionDataMSSQL = new String[] {
            "CREATE TABLE mangoSessionData (sessionId NVARCHAR(120), contextPath NVARCHAR(60), virtualHost NVARCHAR(60), lastNode NVARCHAR(60), accessTime BIGINT, lastAccessTime BIGINT, createTime BIGINT, cookieTime BIGINT, lastSavedTime BIGINT, expiryTime BIGINT, maxInterval BIGINT, userId INT, primary key (sessionId, contextPath, virtualHost));",
            "CREATE INDEX mangoSessionDataExpiryIndex ON mangoSessionData (expiryTime);",
            "CREATE INDEX mangoSessionDataSessionIndex ON mangoSessionData (sessionId, contextPath);"
    };

    @Override
    protected String getNewSchemaVersion() {
        return "30";
    }

    @Override
    public ExtendedJdbcTemplate getEjt() {
        return ejt;
    }

    @Override
    public DSLContext getCreate() {
        return create;
    }

    @Override
    public TransactionTemplate getTransactionTemplate() {
        return super.getTransactionTemplate();
    }
}
