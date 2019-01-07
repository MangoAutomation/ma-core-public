/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
 * @author Terry Packer
 *
 */
@Service
public class DataSourceService extends AbstractVOService<DataSourceVO<?>, DataSourceDao<DataSourceVO<?>>> {

    @Autowired
    public DataSourceService(DataSourceDao<DataSourceVO<?>> dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user) {
        return Permissions.hasDataSourcePermission(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, DataSourceVO<?> vo) {
        return Permissions.hasDataSourcePermission(user, vo);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, DataSourceVO<?> vo) {
        return Permissions.hasDataSourcePermission(user, vo);
    }

    /**
     * Get a definition for a data source
     * @param dataSourceType
     * @param user
     * @return
     */
    public DataSourceDefinition getDefinition(String dataSourceType, User user) throws NotFoundException, PermissionException {
        ensureCreatePermission(user);
        DataSourceDefinition def = ModuleRegistry.getDataSourceDefinition(dataSourceType);
        if(def == null)
            throw new NotFoundException();
        return def;
    }
    

}
