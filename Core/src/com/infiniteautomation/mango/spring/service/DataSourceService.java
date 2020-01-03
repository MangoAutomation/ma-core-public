/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointTagsDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.DataSourcePermissionDefinition;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.DataPointVO.PurgeTypes;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.validation.StringValidation;

/**
 * 
 * @author Terry Packer
 *
 */
@Service
public class DataSourceService<T extends DataSourceVO<T>> extends AbstractVOService<T, DataSourceDao<T>> {

    private final DataPointService dataPointService;
    private final DataSourcePermissionDefinition createPermission;
    
    @Autowired
    public DataSourceService(DataSourceDao<T> dao, PermissionService permissionService, DataPointService dataPointService, DataSourcePermissionDefinition createPermission) {
        super(dao, permissionService);
        this.dataPointService = dataPointService;
        this.createPermission = createPermission;
    }

    @Override
    public Set<Role> getCreatePermissionRoles() {
        return createPermission.getRoles();
    }
    
    @Override
    @EventListener
    protected void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {
        //So we don't have to restart it
        for(DataSourceRT<?> rt : Common.runtimeManager.getRunningDataSources()) {
            if(rt.getVo().getEditRoles().contains(event.getRole().getRole())) {
                Set<Role> newEditRoles = new HashSet<>(rt.getVo().getEditRoles());
                newEditRoles.remove(event.getRole().getRole());
                rt.getVo().setEditRoles(Collections.unmodifiableSet(newEditRoles));
            }
        }
    }
    
    @Override
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        return permissionService.hasDataSourcePermission(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, T vo) {
        return permissionService.hasDataSourcePermission(user, vo);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, T vo) {
        return permissionService.hasDataSourcePermission(user, vo);
    }

    @Override
    public T insert(T vo)
            throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        //Ensure they can create a list
        ensureCreatePermission(user, vo);
        
        //Ensure we don't presume to exist
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());
        
        ensureValid(vo, user);
        Common.runtimeManager.insertDataSource(vo);
        
        return vo;
    }
    
    
    @Override
    public T update(T existing, T vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        ensureEditPermission(user, existing);
        
        //Ensure matching data source types
        if(!StringUtils.equals(existing.getDefinition().getDataSourceTypeName(), vo.getDefinition().getDataSourceTypeName())) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("definition.dataSourceTypeName", "validate.incompatibleDataSourceType");
            throw new ValidationException(result);
        }
        
        vo.setId(existing.getId());
        ensureValid(existing, vo, user);
        Common.runtimeManager.updateDataSource(existing, vo);
        return vo;
    }
    
    @Override
    public T delete(String xid)
            throws PermissionException, NotFoundException {
        T vo = get(xid);
        PermissionHolder user = Common.getUser();
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
    public DataSourceDefinition<T> getDefinition(String dataSourceType, User user) throws NotFoundException, PermissionException {
        permissionService.ensureDataSourcePermission(user);
        @SuppressWarnings("unchecked")
        DataSourceDefinition<T> def = ModuleRegistry.getDataSourceDefinition(dataSourceType);
        if(def == null)
            throw new NotFoundException();
        return def;
    }
    
    /**
     * Enable/disable/restart a data source
     * @param xid
     * @param enabled - Enable or disable the data source
     * @param restart - Restart the data source, enabled must equal true
     */
    public void restart(String xid, boolean enabled, boolean restart) {
        T vo = get(xid);
        T existing = vo.copy();
        PermissionHolder user = Common.getUser();
        ensureEditPermission(user, vo);
        if (enabled && restart) {
            vo.setEnabled(true);
            Common.runtimeManager.updateDataSource(existing, vo); //saving will restart it
        } else if(vo.isEnabled() != enabled) {
            vo.setEnabled(enabled);
            Common.runtimeManager.updateDataSource(existing, vo);
        }
    }

    /**
     * Copy a data source and optionally its points
     * @param xid
     * @param copyXid
     * @param copyName
     * @param copyDeviceName
     * @param enabled
     * @param copyPoints
     * @return
     * @throws PermissionException
     * @throws NotFoundException
     */
    public T copy(String xid, String copyXid, String copyName, String copyDeviceName, boolean enabled, boolean copyPoints) throws PermissionException, NotFoundException {
        T existing = get(xid);
        PermissionHolder user = Common.getUser();
        ensureCreatePermission(user, existing);
        //Determine the new name
        String newName;
        if(StringUtils.isEmpty(copyName)) {
            newName = StringUtils.abbreviate(
                    TranslatableMessage.translate(Common.getTranslations(), "common.copyPrefix", existing.getName()), 40);
        }else {
            newName = copyName;
        }
        //Determine the new xid
        String newXid;
        if(StringUtils.isEmpty(copyXid)) {
            newXid = DataSourceDao.getInstance().generateUniqueXid();
        }else {
            newXid = copyXid;
        }
        
        String newDeviceName;
        if(StringUtils.isEmpty(copyDeviceName)) {
            newDeviceName = existing.getName();
        }else {
            newDeviceName = copyDeviceName;
        }
        //Ensure device name is valid
        if (StringValidation.isLengthGreaterThan(newDeviceName, 255)) {
            ProcessResult result = new ProcessResult();
            result.addMessage("deviceName", new TranslatableMessage("validate.notLongerThan", 255));
            throw new ValidationException(result);
        }
        
        T copy = existing.copy();
        copy.setId(Common.NEW_ID);
        copy.setName(newName);
        copy.setXid(newXid);
        copy.setEnabled(enabled);
        ensureValid(copy, user);
        
        //Save it
        Common.runtimeManager.insertDataSource(copy);
        
        if(copyPoints) {
            copyDataSourcePoints(existing.getId(), copy, newDeviceName);
        }
        return get(newXid);
    }
   
    /**
     * 
     * @param dataSourceId - original id to get points from
     * @param dataSourceCopy
     * @param newDeviceName - if null will use data source copy's name
     * @param user
     */
    private void copyDataSourcePoints(int dataSourceId, DataSourceVO<?> dataSourceCopy, String newDeviceName) {
        // Copy the points by getting each point without any relational data and loading it as we need it
        for (DataPointVO dataPoint : dataPointService.getDataPoints(dataSourceId)) {
            DataPointVO dataPointCopy = dataPoint.copy();
            dataPointCopy.setId(Common.NEW_ID);
            dataPointCopy.setXid(dataPointService.getDao().generateUniqueXid());
            dataPointCopy.setName(dataPoint.getName());
            dataPointCopy.setDeviceName(newDeviceName != null ? newDeviceName : dataSourceCopy.getName());
            dataPointCopy.setDataSourceId(dataSourceCopy.getId());
            dataPointCopy.setEnabled(dataPoint.isEnabled());
            
            //Copy Tags
            dataPointCopy.setTags(DataPointTagsDao.getInstance().getTagsForDataPointId(dataPoint.getId()));
            //Copy event detectors and simulate them being new
            dataPointCopy.setEventDetectors(EventDetectorDao.getInstance().getWithSource(dataPoint.getId(), dataPointCopy));
            for (AbstractPointEventDetectorVO<?> ped : dataPointCopy.getEventDetectors()) {
                ped.setId(Common.NEW_ID);
                ped.setXid(EventDetectorDao.getInstance().generateUniqueXid());
            }
            dataPointService.insert(dataPointCopy);
        }
    }

    @Override
    public ProcessResult validate(T vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        boolean owner = user != null ? permissionService.hasDataSourcePermission(user) : false;
        permissionService.validateVoRoles(response, "editRoles", user, owner, null, vo.getEditRoles());
        //Allow module to define validation logic
        vo.getDefinition().validate(response, vo, user);
        return response;
    }
    
    @Override
    public ProcessResult validate(T existing, T vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        //If we have global data source permission then we are the 'owner' and don't need any edit permission for this source
        boolean owner = user != null ? permissionService.hasDataSourcePermission(user) : false;
        permissionService.validateVoRoles(response, "editRoles", user, owner, existing.getEditRoles(), vo.getEditRoles());
        //Allow module to define validation logic
        vo.getDefinition().validate(response, existing, vo, user);
        return response;
    }
    
    protected ProcessResult commonValidation(T vo, PermissionHolder user) {
        ProcessResult response = super.validate(vo, user);
        if (vo.isPurgeOverride()) {
            if (vo.getPurgeType() != PurgeTypes.DAYS && vo.getPurgeType() != PurgeTypes.MONTHS && vo.getPurgeType() != PurgeTypes.WEEKS
                    && vo.getPurgeType() != PurgeTypes.YEARS)
                response.addContextualMessage("purgeType", "validate.invalidValue");
            if (vo.getPurgePeriod() <= 0)
                response.addContextualMessage("purgePeriod", "validate.greaterThanZero");
        }
        
        return response;
    }
    
}
