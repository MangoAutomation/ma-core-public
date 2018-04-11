/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.db.dao;

import java.sql.Types;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;

public class InstalledModulesDao extends BaseDao {
    public static final InstalledModulesDao instance = new InstalledModulesDao();
    
    private static final String DELETE_ALL = "DELETE FROM installedModules";
    private static final String DELETE_MODULE = "DELETE FROM installedModules WHERE name=?";
    private static final String INSERT_MODULE = "INSERT INTO installedModules (name, version) VALUES (?,?)";
    
    public void removeModuleVersion(String name) {
        ejt.update(DELETE_MODULE, name);
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
}
