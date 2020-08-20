/*
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
 * @author Jared Wiltshire
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

            //Setup roles
            createRolesTables(out);
            createSystemPermissions(out);
            convertUsers(out);
            convertSystemSettingsPermissions(out);
            convertDataSources(out);
            convertDataPoints(out);
            convertJsonData(out);
            convertMailingLists(out);
            convertFileStores(out);
            upgradeEventHandlers(out);
            upgradeEventDetectors(out);
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

    private void createSystemPermissions(OutputStream out) {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), createSystemPermissionsMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), createSystemPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), createSystemPermissionsMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), createSystemPermissionsSQL);
        runScript(scripts, out);
    }
    private void dropTemplates(OutputStream out) {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dropTemplatesSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dropTemplatesSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dropTemplatesSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dropTemplatesSQL);
        runScript(scripts, out);
    }

    private void dropPointHierarchy(OutputStream out) {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dropPointHierarchySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dropPointHierarchySQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dropPointHierarchySQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dropPointHierarchySQL);
        runScript(scripts, out);
    }

    private void dropUserEvents(OutputStream out) {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dropUserEventsSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dropUserEventsSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dropUserEventsSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dropUserEventsSQL);
        runScript(scripts, out);
    }

    private void createRolesTables(OutputStream out) {
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

    private void convertUsers(OutputStream out) {
        //Move current permissions to roles
        ejt.query("SELECT id, permissions FROM users", rs -> {
            int userId = rs.getInt(1);
            //Get user's current permissions
            Set<String> legacyRoleNames = PermissionService.explodeLegacyPermissionGroups(rs.getString(2));
            Set<Role> savedRoles = legacyRoleNames.stream().map(roleName -> new Role(Common.NEW_ID, roleName))
                    .map(this::getOrCreateRole).collect(Collectors.toSet());
            // ensure they have the user role
            savedRoles.add(PermissionHolder.USER_ROLE);

            for (Role role : savedRoles) {
                //Add a mapping
                ejt.doInsert("INSERT INTO userRoleMappings (roleId, userId) VALUES (?,?)",
                        role.getId(),
                        userId);
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

    private void convertSystemSettingsPermissions(OutputStream out) {
        //Check all permissions
        for (PermissionDefinition def : ModuleRegistry.getDefinitions(PermissionDefinition.class)) {
            //Move to roles and map them
            ejt.query("SELECT settingValue FROM systemSettings WHERE settingName=?", new Object[] {def.getPermissionTypeName()}, rs -> {
                //Add role/mapping
                Integer id = getOrCreatePermission(PermissionMigration.parseLegacyPermission(rs.getString(1))).getId();
                ejt.doInsert("INSERT INTO systemPermissions (permissionType,permissionId) VALUES (?,?)", new Object[] {def.getPermissionTypeName(), id});
            });
            //Delete the setting
            ejt.update("DELETE FROM systemSettings WHERE settingName=?", def.getPermissionTypeName());
        }
    }

    /**
     * Convert data points AFTER data sources so we can reference the readPermissionId and editPermissionId
     */
    private void convertDataPoints(OutputStream out) {
        //Create permission columns
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataPointPermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataPointPermissionH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataPointPermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataPointPermissionMySQL);
        runScript(scripts, out);

        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, setPermission, dataSourceId FROM dataPoints", rs -> {
            int voId = rs.getInt(1);
            //Add role/mapping
            MangoPermission readPermissions = PermissionMigration.parseLegacyPermission(rs.getString(2));
            Integer read = getOrCreatePermission(readPermissions).getId();
            MangoPermission setPermissions = PermissionMigration.parseLegacyPermission(rs.getString(3));
            Integer set = getOrCreatePermission(setPermissions).getId();
            int dataSourceId = rs.getInt(4);
            int edit = ejt.queryForInt("SELECT editPermissionId from dataSources WHERE id=?", new Object[] {dataSourceId}, 0);
            ejt.update("UPDATE dataPoints SET readPermissionId=?,editPermissionId=?,setPermissionId=? WHERE id=?", read, edit, set, voId);
        });

        //Restrict to NOT NULL
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataPointPermissionNotNullMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataPointPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataPointPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataPointPermissionNotNull);
        runScript(scripts, out);

        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataPointsMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataPointsH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataPointsMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataPointsMySQL);
        runScript(scripts, out);
    }

    private void convertDataSources(OutputStream out) {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataSourcePermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataSourcePermissionH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataSourcePermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataSourcePermissionMySQL);
        runScript(scripts, out);

        //Move current permissions to roles
        ejt.query("SELECT id, editPermission FROM dataSources", rs -> {
            int voId = rs.getInt(1);
            //Add role/mapping
            MangoPermission editPermissions = PermissionMigration.parseLegacyPermission(rs.getString(2));
            Integer edit = getOrCreatePermission(editPermissions).getId();
            ejt.update("UPDATE dataSources SET editPermissionId=?,readPermissionId=? WHERE id=?", edit, edit, voId);
        });

        //Restrict to NOT NULL
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataSourcePermissionNotNullMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataSourcePermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataSourcePermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataSourcePermissionNotNull);
        runScript(scripts, out);

        //Drop columns
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), dataSourcesMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), dataSourcesH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), dataSourcesMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), dataSourcesMySQL);
        runScript(scripts, out);
    }

    private void convertJsonData(OutputStream out) {
        //Add permission id columns
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), jsonDataPermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), jsonDataPermissionH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), jsonDataPermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), jsonDataPermissionMySQL);
        runScript(scripts, out);

        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, editPermission, publicData FROM jsonData", rs -> {
            int voId = rs.getInt(1);
            //Add role/mapping
            MangoPermission readPermissions = PermissionMigration.parseLegacyPermission(rs.getString(2));
            if (charToBool(rs.getString(4))) {
                //Is public so add anonymous role
                Set<Set<Role>> newRoles = new HashSet<>(readPermissions.getRoles());
                newRoles.add(Collections.singleton(PermissionHolder.ANONYMOUS_ROLE));
                readPermissions = new MangoPermission(newRoles);
            }
            Integer readId = getOrCreatePermission(readPermissions).getId();
            MangoPermission editPermissions = PermissionMigration.parseLegacyPermission(rs.getString(3));
            Integer editId = getOrCreatePermission(editPermissions).getId();
            ejt.update("UPDATE jsonData SET readPermissionId=?, editPermissionId=? WHERE id=?", readId, editId, voId);
        });

        //DROP publicData column
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), jsonDataDropPublicDataSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), jsonDataDropPublicDataSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), jsonDataDropPublicDataSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), jsonDataDropPublicDataSQL);
        runScript(scripts, out);

        //Restrict to NOT NULL
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), jsonDataPermissionNotNullMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), jsonDataPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), jsonDataPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), jsonDataPermissionNotNull);
        runScript(scripts, out);

        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), jsonDataDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), jsonDataDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), jsonDataDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), jsonDataDropPermissionsSQL);
        runScript(scripts, out);
    }

    private void convertMailingLists(OutputStream out) {
        //Add permission id columns
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mailingListsPermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), mailingListsPermissionH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mailingListsPermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), mailingListsPermissionMySQL);
        runScript(scripts, out);

        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, editPermission FROM mailingLists", rs -> {
            int voId = rs.getInt(1);
            //Add role/mapping
            MangoPermission readPermissions = PermissionMigration.parseLegacyPermission(rs.getString(2));
            Integer readId = getOrCreatePermission(readPermissions).getId();
            MangoPermission editPermissions = PermissionMigration.parseLegacyPermission(rs.getString(3));
            Integer editId = getOrCreatePermission(editPermissions).getId();
            ejt.update("UPDATE mailingLists SET readPermissionId=?, editPermissionId=? WHERE id=?", readId, editId, voId);
        });

        //Restrict to NOT NULL
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mailingListsPermissionNotNullMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), mailingListsPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mailingListsPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), mailingListsPermissionNotNull);
        runScript(scripts, out);

        //Drop old columns
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mailingListsDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), mailingListsDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mailingListsDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), mailingListsDropPermissionsSQL);
        runScript(scripts, out);
    }

    private void upgradeEventDetectors(OutputStream out) {
        //Add permission id columns
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), eventDetectorsPermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), eventDetectorsPermissionH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), eventDetectorsPermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), eventDetectorsPermissionMySQL);
        runScript(scripts, out);

        //set permission to superadmin
        Integer readId = getOrCreatePermission(MangoPermission.superadminOnly()).getId();
        Integer editId = getOrCreatePermission(MangoPermission.superadminOnly()).getId();
        ejt.update("UPDATE eventDetectors SET readPermissionId=?, editPermissionId=?", readId, editId);

        //Restrict to NOT NULL
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(),eventDetectorsPermissionNotNullMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), eventDetectorsPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), eventDetectorsPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), eventDetectorsPermissionNotNull);
        runScript(scripts, out);
    }

    private void upgradeEventHandlers(OutputStream out) {
        //Add permission id columns
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), eventHandlersPermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), eventHandlersPermissionH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), eventHandlersPermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), eventHandlersPermissionMySQL);
        runScript(scripts, out);

        //set permission to superadmin
        Integer readId = getOrCreatePermission(MangoPermission.superadminOnly()).getId();
        Integer editId = getOrCreatePermission(MangoPermission.superadminOnly()).getId();
        ejt.update("UPDATE eventHandlers SET readPermissionId=?, editPermissionId=?", readId, editId);

        //Restrict to NOT NULL
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), eventHandlersPermissionNotNullMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), eventHandlersPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), eventHandlersPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), eventHandlersPermissionNotNull);
        runScript(scripts, out);
    }

    private void convertFileStores(OutputStream out) {
        //Add permission id columns
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), fileStoresPermissionMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), fileStoresPermissionH2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), fileStoresPermissionMSSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), fileStoresPermissionMySQL);
        runScript(scripts, out);

        //Move current permissions to roles
        ejt.query("SELECT id, readPermission, writePermission FROM fileStores", rs -> {
            int voId = rs.getInt(1);
            //Add role/mapping
            MangoPermission readPermissions = PermissionMigration.parseLegacyPermission(rs.getString(2));
            Integer readId = getOrCreatePermission(readPermissions).getId();
            MangoPermission writePermissions = PermissionMigration.parseLegacyPermission(rs.getString(3));
            Integer writeId = getOrCreatePermission(writePermissions).getId();
            ejt.update("UPDATE fileStores SET readPermissionId=?, writePermissionId=? WHERE id=?", readId, writeId, voId);
        });

        //Restrict to NOT NULL
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), fileStoresPermissionNotNullMySQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), fileStoresPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), fileStoresPermissionNotNull);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), fileStoresPermissionNotNull);
        runScript(scripts, out);

        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), fileStoresDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), fileStoresDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), fileStoresDropPermissionsSQL);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), fileStoresDropPermissionsSQL);
        runScript(scripts, out);
    }

    //
    //System Permissions
    //
    private final String[] createSystemPermissionsSQL = new String[] {
            "CREATE TABLE systemPermissions (permissionType VARCHAR(255), permissionId INT NOT NULL);",
            "ALTER TABLE systemPermissions ADD CONSTRAINT systemPermissionsFk1 FOREIGN KEY (permissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE systemPermissions ADD CONSTRAINT permissionTypeUn1 UNIQUE(permissionType);"
    };
    private final String[] createSystemPermissionsMySQL = new String[] {
            "CREATE TABLE systemPermissions (permissionType VARCHAR(255), permissionId INT NOT NULL)engine=InnoDB;",
            "ALTER TABLE systemPermissions ADD CONSTRAINT systemPermissionsFk1 FOREIGN KEY (permissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE systemPermissions ADD CONSTRAINT permissionTypeUn1 UNIQUE(permissionType);"
    };
    private final String[] createSystemPermissionsMSSQL = new String[] {
            "CREATE TABLE systemPermissions (permissionType NVARCHAR(255), permissionId INT NOT NULL);",
            "ALTER TABLE systemPermissions ADD CONSTRAINT systemPermissionsFk1 FOREIGN KEY (permissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE systemPermissions ADD CONSTRAINT permissionTypeUn1 UNIQUE(permissionType);"

    };

    //
    // Adding json data columns
    //
    private final String[] jsonDataColumnsSql = new String[]{
            "ALTER TABLE dataPoints ADD COLUMN jsonData longtext;",
            "ALTER TABLE dataSources ADD COLUMN jsonData longtext;",
            "ALTER TABLE eventDetectors ADD COLUMN jsonData longtext;"
    };
    private final String[] jsonDataColumnsMySQL = new String[]{
            "ALTER TABLE dataPoints ADD COLUMN jsonData JSON;",
            "ALTER TABLE dataSources ADD COLUMN jsonData JSON;",
            "ALTER TABLE eventDetectors ADD COLUMN jsonData JSON;"
    };
    private final String[] jsonDataColumnsMSSQL = new String[]{
            "ALTER TABLE dataPoints ADD COLUMN jsonData ntext;",
            "ALTER TABLE dataSources ADD COLUMN jsonData ntext;",
            "ALTER TABLE eventDetectors ADD COLUMN jsonData ntext;"
    };

    //Data source fix
    private final String[] alterDataSourceNameColumnMySQL = new String[] {
            "ALTER TABLE dataSources MODIFY COLUMN name VARCHAR(255) NOT NULL;"
    };

    //Data point indexes
    private final String[] dataPointIndexes = new String[] {
            "CREATE INDEX deviceNameNameIdIndex ON dataPoints (deviceName ASC, name ASC, id ASC);"
    };


    //Templates
    private final String[] dropTemplatesSQL = new String[] {
            "DROP TABLE templates;",
    };

    //Roles
    private final String[] createRolesSQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id));",
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",

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
    private final String[] createRolesMySQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id)) engine=InnoDB;",
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",

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
    private final String[] createRolesMSSQL = new String[] {
            "CREATE TABLE roles (id int not null auto_increment, xid varchar(100) not null, name varchar(255) not null, primary key (id));",
            "ALTER TABLE roles ADD CONSTRAINT rolesUn1 UNIQUE (xid);",

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
    private final String[] defaultRolesSQL = new String[] {
            "INSERT INTO roles (id, xid, name) VALUES (1, 'superadmin', 'Superadmins');",
            "INSERT INTO roles (id, xid, name) VALUES (2, 'user', 'Users');",
            "INSERT INTO roles (id, xid, name) VALUES (3, 'anonymous', 'Anonymous role');"
    };

    //Point Hierarchy
    private final String[] dropPointHierarchySQL = new String[] {
            "DROP TABLE dataPointHierarchy;",
    };

    //User Events
    private final String[] dropUserEventsSQL = new String[] {
            "DROP TABLE userEvents;",
    };

    //Users
    private final String[] userSQL = new String[] {
            "ALTER TABLE users DROP COLUMN permissions;",
    };

    //
    //Data points
    //
    private final String[] dataPointsH2 = new String[] {
            "DROP INDEX dataPointsPermissionIndex;",
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;",
    };
    private final String[] dataPointPermissionH2 = new String[] {
            "ALTER TABLE dataPoints DROP CONSTRAINT dataPointsFk2;",
            "ALTER TABLE dataPoints ADD COLUMN readPermissionId INT;",
            "ALTER TABLE dataPoints ADD COLUMN editPermissionId INT;",
            "ALTER TABLE dataPoints ADD COLUMN setPermissionId INT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk4 FOREIGN KEY (setPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] dataPointPermissionNotNull = new String[] {
            "ALTER TABLE dataPoints ALTER COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE dataPoints ALTER COLUMN editPermissionId INT NOT NULL;",
            "ALTER TABLE dataPoints ALTER COLUMN setPermissionId INT NOT NULL;"
    };

    private final String[] dataPointsMySQL = new String[] {
            "ALTER TABLE dataPoints DROP INDEX dataPointsPermissionIndex;",
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;",
    };

    private final String[] dataPointPermissionNotNullMySQL = new String[] {
            "ALTER TABLE dataPoints MODIFY COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE dataPoints MODIFY COLUMN editPermissionId INT NOT NULL;",
            "ALTER TABLE dataPoints MODIFY COLUMN setPermissionId INT NOT NULL;"
    };

    private final String[] dataPointPermissionMySQL = new String[] {
            "ALTER TABLE dataPoints DROP FOREIGN KEY dataPointsFk2;",
            "ALTER TABLE dataPoints ADD COLUMN readPermissionId INT;",
            "ALTER TABLE dataPoints ADD COLUMN editPermissionId INT;",
            "ALTER TABLE dataPoints ADD COLUMN setPermissionId INT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk4 FOREIGN KEY (setPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] dataPointsMSSQL = new String[] {
            "DROP INDEX dataPoints.dataPointsPermissionIndex;",
            "ALTER TABLE dataPoints DROP COLUMN readPermission;",
            "ALTER TABLE dataPoints DROP COLUMN setPermission;",
            "ALTER TABLE dataPoints DROP COLUMN pointFolderId;",
            "ALTER TABLE dataPoints DROP COLUMN templateId;",
    };
    private final String[] dataPointPermissionMSSQL = new String[] {
            "ALTER TABLE dataPoints DROP CONSTRAINT dataPointsFk2;",
            "ALTER TABLE dataPoints ADD COLUMN readPermissionId INT;",
            "ALTER TABLE dataPoints ADD COLUMN editPermissionId INT;",
            "ALTER TABLE dataPoints ADD COLUMN setPermissionId INT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk4 FOREIGN KEY (setPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    //
    //Data sources
    //
    private final String[] dataSourcesH2 = new String[] {
            "DROP INDEX dataSourcesPermissionIndex;",
            "ALTER TABLE dataSources DROP COLUMN editPermission;",
    };
    private final String[] dataSourcePermissionH2 = new String[] {
            "ALTER TABLE dataSources ADD COLUMN readPermissionId INT;",
            "ALTER TABLE dataSources ADD COLUMN editPermissionId INT;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] dataSourcePermissionNotNull = new String[] {
            "ALTER TABLE dataSources ALTER COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE dataSources ALTER COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] dataSourcesMySQL = new String[] {
            "ALTER TABLE dataPoints DROP INDEX dataSourcesPermissionIndex;",
            "ALTER TABLE dataSources DROP COLUMN editPermission;",
    };
    private final String[] dataSourcePermissionMySQL = new String[] {
            "ALTER TABLE dataSources ADD COLUMN readPermissionId INT;",
            "ALTER TABLE dataSources ADD COLUMN editPermissionId INT;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] dataSourcePermissionNotNullMySQL = new String[] {
            "ALTER TABLE dataSources MODIFY COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE dataSources MODIFY COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] dataSourcesMSSQL = new String[] {
            "DROP INDEX dataSources.dataSourcesPermissionIndex;",
            "ALTER TABLE dataSources DROP COLUMN editPermission;",
    };

    private final String[] dataSourcePermissionMSSQL = new String[] {
            "ALTER TABLE dataSources ADD COLUMN readPermissionId INT;",
            "ALTER TABLE dataSources ADD COLUMN editPermissionId INT;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE dataSources ADD CONSTRAINT dataSourcesFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    //
    //Mailing lists
    //
    private final String[] mailingListsDropPermissionsSQL = new String[] {
            "ALTER TABLE mailingLists DROP COLUMN readPermission;",
            "ALTER TABLE mailingLists DROP COLUMN editPermission;"
    };
    private final String[]  mailingListsPermissionH2 = new String[] {
            "ALTER TABLE  mailingLists ADD COLUMN readPermissionId INT;",
            "ALTER TABLE  mailingLists ADD COLUMN editPermissionId INT;",
            "ALTER TABLE  mailingLists ADD CONSTRAINT mailingListsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE  mailingLists ADD CONSTRAINT mailingListsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };
    private final String[] mailingListsPermissionMySQL = new String[] {
            "ALTER TABLE mailingLists ADD COLUMN readPermissionId INT;",
            "ALTER TABLE mailingLists ADD COLUMN editPermissionId INT;",
            "ALTER TABLE mailingLists ADD CONSTRAINT mailingListsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE mailingLists ADD CONSTRAINT mailingListsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] mailingListsPermissionNotNullMySQL = new String[] {
            "ALTER TABLE mailingLists MODIFY COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE mailingLists MODIFY COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] mailingListsPermissionNotNull = new String[] {
            "ALTER TABLE mailingLists ALTER COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE mailingLists ALTER COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] mailingListsPermissionMSSQL = new String[] {
            "ALTER TABLE mailingLists ADD COLUMN readPermissionId INT;",
            "ALTER TABLE mailingLists ADD COLUMN editPermissionId INT;",
            "ALTER TABLE mailingLists ADD CONSTRAINT mailingListsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE mailingLists ADD CONSTRAINT mailingListsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    //
    //JSON Data table
    //
    private final String[] jsonDataDropPermissionsSQL = new String[] {
            "ALTER TABLE jsonData DROP COLUMN readPermission;",
            "ALTER TABLE jsonData DROP COLUMN editPermission;"
    };
    private final String[]  jsonDataPermissionH2 = new String[] {
            "ALTER TABLE  jsonData ADD COLUMN readPermissionId INT;",
            "ALTER TABLE  jsonData ADD COLUMN editPermissionId INT;",
            "ALTER TABLE  jsonData ADD CONSTRAINT jsonDataFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE  jsonData ADD CONSTRAINT jsonDataFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };
    private final String[] jsonDataPermissionMySQL = new String[] {
            "ALTER TABLE jsonData ADD COLUMN readPermissionId INT;",
            "ALTER TABLE jsonData ADD COLUMN editPermissionId INT;",
            "ALTER TABLE jsonData ADD CONSTRAINT jsonDataFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE jsonData ADD CONSTRAINT jsonDataFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] jsonDataPermissionNotNullMySQL = new String[] {
            "ALTER TABLE jsonData MODIFY COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE jsonData MODIFY COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] jsonDataPermissionNotNull = new String[] {
            "ALTER TABLE jsonData ALTER COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE jsonData ALTER COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] jsonDataPermissionMSSQL = new String[] {
            "ALTER TABLE jsonData ADD COLUMN readPermissionId INT;",
            "ALTER TABLE jsonData ADD COLUMN editPermissionId INT;",
            "ALTER TABLE jsonData ADD CONSTRAINT jsonDataFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE jsonData ADD CONSTRAINT jsonDataFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] jsonDataDropPublicDataSQL = new String[] {
            "ALTER TABLE jsonData DROP COLUMN publicData;",
    };

    //
    //File store
    //
    private final String[] fileStoresDropPermissionsSQL = new String[] {
            "ALTER TABLE fileStores DROP COLUMN readPermission;",
            "ALTER TABLE fileStores DROP COLUMN writePermission;"
    };
    private final String[]  fileStoresPermissionH2 = new String[] {
            "ALTER TABLE  fileStores ADD COLUMN readPermissionId INT;",
            "ALTER TABLE  fileStores ADD COLUMN writePermissionId INT;",
            "ALTER TABLE  fileStores ADD CONSTRAINT fileStoresFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE  fileStores ADD CONSTRAINT fileStoresFk2 FOREIGN KEY (writePermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };
    private final String[] fileStoresPermissionMySQL = new String[] {
            "ALTER TABLE fileStores ADD COLUMN readPermissionId INT;",
            "ALTER TABLE fileStores ADD COLUMN writePermissionId INT;",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresFk2 FOREIGN KEY (writePermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] fileStoresPermissionNotNullMySQL = new String[] {
            "ALTER TABLE fileStores MODIFY COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE fileStores MODIFY COLUMN writePermissionId INT NOT NULL;"
    };

    private final String[] fileStoresPermissionNotNull = new String[] {
            "ALTER TABLE fileStores ALTER COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE fileStores ALTER COLUMN writePermissionId INT NOT NULL;"
    };

    private final String[] fileStoresPermissionMSSQL = new String[] {
            "ALTER TABLE fileStores ADD COLUMN readPermissionId INT;",
            "ALTER TABLE fileStores ADD COLUMN writePermissionId INT;",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresFk2 FOREIGN KEY (writePermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    //
    //Session data
    //
    private final String[] sessionDataMySQL = new String[] {
            "CREATE TABLE mangoSessionData (sessionId VARCHAR(120), contextPath VARCHAR(60), virtualHost VARCHAR(60), lastNode VARCHAR(60), accessTime BIGINT, lastAccessTime BIGINT, createTime BIGINT, cookieTime BIGINT, lastSavedTime BIGINT, expiryTime BIGINT, maxInterval BIGINT, userId INT, primary key (sessionId, contextPath, virtualHost))engine=InnoDB;",
            "CREATE INDEX mangoSessionDataExpiryIndex ON mangoSessionData (expiryTime);",
            "CREATE INDEX mangoSessionDataSessionIndex ON mangoSessionData (sessionId, contextPath);"
    };

    private final String[] sessionDataSQL = new String[] {
            "CREATE TABLE mangoSessionData (sessionId VARCHAR(120), contextPath VARCHAR(60), virtualHost VARCHAR(60), lastNode VARCHAR(60), accessTime BIGINT, lastAccessTime BIGINT, createTime BIGINT, cookieTime BIGINT, lastSavedTime BIGINT, expiryTime BIGINT, maxInterval BIGINT, userId INT, primary key (sessionId, contextPath, virtualHost));",
            "CREATE INDEX mangoSessionDataExpiryIndex ON mangoSessionData (expiryTime);",
            "CREATE INDEX mangoSessionDataSessionIndex ON mangoSessionData (sessionId, contextPath);"
    };

    private final String[] sessionDataMSSQL = new String[] {
            "CREATE TABLE mangoSessionData (sessionId NVARCHAR(120), contextPath NVARCHAR(60), virtualHost NVARCHAR(60), lastNode NVARCHAR(60), accessTime BIGINT, lastAccessTime BIGINT, createTime BIGINT, cookieTime BIGINT, lastSavedTime BIGINT, expiryTime BIGINT, maxInterval BIGINT, userId INT, primary key (sessionId, contextPath, virtualHost));",
            "CREATE INDEX mangoSessionDataExpiryIndex ON mangoSessionData (expiryTime);",
            "CREATE INDEX mangoSessionDataSessionIndex ON mangoSessionData (sessionId, contextPath);"
    };

    //
    //Event Detectors
    //
    private final String[] eventDetectorsPermissionH2 = new String[] {
            "ALTER TABLE  eventDetectors ADD COLUMN readPermissionId INT;",
            "ALTER TABLE  eventDetectors ADD COLUMN editPermissionId INT;",
            "ALTER TABLE  eventDetectors ADD CONSTRAINT eventDetectorsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE  eventDetectors ADD CONSTRAINT eventDetectorsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };
    private final String[] eventDetectorsPermissionMySQL = new String[] {
            "ALTER TABLE eventDetectors ADD COLUMN readPermissionId INT;",
            "ALTER TABLE eventDetectors ADD COLUMN editPermissionId INT;",
            "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] eventDetectorsPermissionNotNullMySQL = new String[] {
            "ALTER TABLE eventDetectors MODIFY COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE eventDetectors MODIFY COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] eventDetectorsPermissionNotNull = new String[] {
            "ALTER TABLE eventDetectors ALTER COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE eventDetectors ALTER COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] eventDetectorsPermissionMSSQL = new String[] {
            "ALTER TABLE eventDetectors ADD COLUMN readPermissionId INT;",
            "ALTER TABLE eventDetectors ADD COLUMN editPermissionId INT;",
            "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsFk1 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsFk2 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    //
    //Event Handlers
    //
    private final String[] eventHandlersPermissionH2 = new String[] {
            "ALTER TABLE  eventHandlers ADD COLUMN readPermissionId INT;",
            "ALTER TABLE  eventHandlers ADD COLUMN editPermissionId INT;",
            "ALTER TABLE  eventHandlers ADD CONSTRAINT eventHandlersFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE  eventHandlers ADD CONSTRAINT eventHandlersFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };
    private final String[] eventHandlersPermissionMySQL = new String[] {
            "ALTER TABLE eventHandlers ADD COLUMN readPermissionId INT;",
            "ALTER TABLE eventHandlers ADD COLUMN editPermissionId INT;",
            "ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    private final String[] eventHandlersPermissionNotNullMySQL = new String[] {
            "ALTER TABLE eventHandlers MODIFY COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE eventHandlers MODIFY COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] eventHandlersPermissionNotNull = new String[] {
            "ALTER TABLE eventHandlers ALTER COLUMN readPermissionId INT NOT NULL;",
            "ALTER TABLE eventHandlers ALTER COLUMN editPermissionId INT NOT NULL;"
    };

    private final String[] eventHandlersPermissionMSSQL = new String[] {
            "ALTER TABLE eventHandlers ADD COLUMN readPermissionId INT;",
            "ALTER TABLE eventHandlers ADD COLUMN editPermissionId INT;",
            "ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersFk2 FOREIGN KEY (readPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;",
            "ALTER TABLE eventHandlers ADD CONSTRAINT eventHandlersFk3 FOREIGN KEY (editPermissionId) REFERENCES permissions(id) ON DELETE RESTRICT;"
    };

    @Override
    protected String getNewSchemaVersion() {
        return "30";
    }

    @Override
    public ExtendedJdbcTemplate getJdbcTemplate() {
        return ejt;
    }

    @Override
    public TransactionTemplate getTransactionTemplate() {
        return super.getTransactionTemplate();
    }
}
