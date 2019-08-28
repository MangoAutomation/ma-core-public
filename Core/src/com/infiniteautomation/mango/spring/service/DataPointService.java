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
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Terry Packer
 *
 */
@Service
public class DataPointService extends AbstractVOService<DataPointVO, DataPointDao> {

    @Autowired
    public DataPointService(DataPointDao dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, DataPointVO vo) {
        return Permissions.hasDataSourcePermission(user, vo.getDataSourceId());
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, DataPointVO vo) {
        return Permissions.hasDataSourcePermission(user, vo.getDataSourceId());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, DataPointVO vo) {
        return Permissions.hasDataPointReadPermission(user, vo);
    }
    
    @Override
    protected DataPointVO insert(DataPointVO vo, PermissionHolder user, boolean full)
            throws PermissionException, ValidationException {
      //Ensure they can create
        ensureCreatePermission(user, vo);

        //Ensure id is not set
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        ensureValid(vo, user);
        Common.runtimeManager.saveDataPoint(vo);
        return vo;
    }
    
    @Override
    protected DataPointVO update(DataPointVO existing, DataPointVO vo,
            PermissionHolder user, boolean full) throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        
        vo.setId(existing.getId());
        ensureValid(existing, vo, user);
        Common.runtimeManager.saveDataPoint(vo);
        return vo;
    }

    @Override
    public DataPointVO delete(String xid, PermissionHolder user)
            throws PermissionException, NotFoundException {
        DataPointVO vo = get(xid, user);
        ensureDeletePermission(user, vo);
        Common.runtimeManager.deleteDataPoint(vo);
        return vo;
    }
    
    /**
     * Enable/Disable/Restart a data point
     * @param xid - xid of point to restart
     * @param enabled - Enable or disable the data point
     * @param restart - Restart the data point, enabled must equal true
     * @param user
     * 
     * @throws NotFoundException
     * @throws PermissionException
     */
    public void enableDisable(String xid, boolean enabled, boolean restart, PermissionHolder user) throws NotFoundException, PermissionException {
        DataPointVO vo = get(xid, user);
        if (enabled && restart) {
            Common.runtimeManager.restartDataPoint(vo);
        } else {
            Common.runtimeManager.enableDataPoint(vo, enabled);
        }
    }
}
