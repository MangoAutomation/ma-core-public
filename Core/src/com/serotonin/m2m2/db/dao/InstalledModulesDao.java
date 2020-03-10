/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;

public class InstalledModulesDao extends BaseDao {
    public static final InstalledModulesDao instance = new InstalledModulesDao();

    private static final String SELECT_VERSION = "SELECT version from installedModules WHERE name=?";
    private static final String DELETE_ALL = "DELETE FROM installedModules";
    private static final String DELETE_MODULE = "DELETE FROM installedModules WHERE name=?";
    private static final String INSERT_MODULE = "INSERT INTO installedModules (name, version) VALUES (?,?)";

    public void removeModuleVersion(String name) {
        ejt.update(DELETE_MODULE, name);
    }

    public Version getModuleVersion(String name) {
        String version = ejt.query(SELECT_VERSION, new Object[] {name}, new ResultSetExtractor<String>() {
            @Override
            public String extractData(ResultSet rs) throws SQLException, DataAccessException {
                if(rs.next()) {
                    return rs.getString(1);
                }else {
                    return null;
                }
            }
        });

        if(version == null) {
            return null;
        }else {
            return Version.valueOf(version);
        }
    }

    public void updateAllModuleVersions() {
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                ejt.update(DELETE_ALL);
                for(Module m : ModuleRegistry.getModules())
                    ejt.doInsert(INSERT_MODULE, new Object[] { m.getName(), m.getVersion().toString() }, new int[] {Types.VARCHAR, Types.VARCHAR});
            }
        });
    }

    /**
     * Update a single module version to its latest
     * @param module
     */
    public void updateModuleVersion(Module module) {
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                ejt.update(DELETE_MODULE, new Object[] {module.getName()});
                ejt.doInsert(INSERT_MODULE, new Object[] { module.getName(), module.getVersion().toString() }, new int[] {Types.VARCHAR, Types.VARCHAR});
            }
        });
    }
}
