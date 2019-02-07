/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
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
public class DataSourceService<T extends DataSourceVO<T>> extends AbstractVOService<T, DataSourceDao<T>> {

    @Autowired
    public DataSourceService(DataSourceDao<T> dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        return Permissions.hasDataSourcePermission(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, T vo) {
        return Permissions.hasDataSourcePermission(user, vo);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, T vo) {
        return Permissions.hasDataSourcePermission(user, vo);
    }

    @Override
    protected T insert(T vo, PermissionHolder user, boolean full)
            throws PermissionException, ValidationException {
        //Ensure they can create a list
        ensureCreatePermission(user, vo);
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());
        
        ensureValid(vo, user);
        Common.runtimeManager.saveDataSource(vo);
        
        return vo;
    }
    
    
    @Override
    protected T update(T existing, T vo,
            PermissionHolder user, boolean full) throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        ensureValid(existing, vo, user);
        Common.runtimeManager.saveDataSource(vo);
        return vo;
    }
    
    @Override
    public T delete(String xid, PermissionHolder user)
            throws PermissionException, NotFoundException {
        T vo = get(xid, user);
        ensureDeletePermission(user, vo);
        Common.runtimeManager.deleteDataSource(vo.getId());
        return vo;
    }
    
    /**
     * Get a definition for a data source
     * @param dataSourceType
     * @param user
     * @return
     */
    public DataSourceDefinition getDefinition(String dataSourceType, User user) throws NotFoundException, PermissionException {
        Permissions.ensureDataSourcePermission(user);
        DataSourceDefinition def = ModuleRegistry.getDataSourceDefinition(dataSourceType);
        if(def == null)
            throw new NotFoundException();
        return def;
    }
    
    /**
     * Enable/disable/restart a data source
     * @param xid
     * @param enabled - Enable or disable the data source
     * @param restart - Restart the data source, enabled must equal true
     * @param user
     */
    public void restart(String xid, boolean enabled, boolean restart, PermissionHolder user) {
        T vo = getFull(xid, user);
        ensureEditPermission(user, vo);
        if (enabled && restart) {
            vo.setEnabled(true);
            Common.runtimeManager.saveDataSource(vo); //saving will restart it
        } else if(vo.isEnabled() != enabled) {
            vo.setEnabled(enabled);
            Common.runtimeManager.saveDataSource(vo);
        }
    }

}
