/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

/**
 * A way to manage only data point event detectors.
 * 
 * This service does NOT reload the source in the runtime manager.  That 
 * must be accomplished elsewhere
 * 
 * @author Terry Packer
 *
 */
@Service
public class EventDetectorsService<T extends AbstractEventDetectorVO<T>> extends AbstractVOService<T, EventDetectorDao<T>>{

    @Autowired
    public EventDetectorsService(EventDetectorDao<T> dao, PermissionService permissionService) {
        super(dao, permissionService);
    }
    
    /**
     * Save a detector an optionally reload its source
     * 
     * @param vo
     * @param restartSource
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T insertAndReload(T vo, boolean restartSource)
            throws PermissionException, ValidationException {
        vo = super.insert(vo);
        
        if(restartSource)
            vo.getDefinition().restartSource(vo);
        
        return vo;
    }
    
    /**
     * Update and optionally restart the source
     * 
     * 
     * @param existing
     * @param vo
     * @param restartSource
     * @return
     */
    public T updateAndReload(String existing, T vo, boolean restartSource) {
        T updated = update(get(existing), vo);
        if(restartSource)
            updated.getDefinition().restartSource(updated);
        return updated;
    }

    
    @Override
    public T delete(String xid)
            throws PermissionException, NotFoundException {
        T vo = super.delete(xid);
        vo.getDefinition().restartSource(vo);
        return vo;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        if(user.hasAdminRole())
            return true;
        return vo.getDefinition().hasCreatePermission(user, vo);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, T vo) {
        if(user.hasAdminRole())
            return true;
        return vo.getDefinition().hasEditPermission(user, vo);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, T vo) {
        if(user.hasAdminRole())
            return true;
        return vo.getDefinition().hasReadPermission(user, vo);
    }
    
    @Override
    public ProcessResult validate(T vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        vo.getDefinition().validate(response, vo, user);
        return response;
    }

    @Override
    public ProcessResult validate(T existing, T vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        vo.getDefinition().validate(response, existing, vo, user);
        return response;
    }
    
    private ProcessResult commonValidation(T vo, PermissionHolder user) {
        ProcessResult response = new ProcessResult();
        if (StringUtils.isBlank(vo.getXid()))
            response.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getXid(), 100))
            response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
        else if (!isXidUnique(vo.getXid(), vo.getId()))
            response.addContextualMessage("xid", "validate.xidUsed");

        // allow blank names
        if (!StringUtils.isBlank(vo.getName())) {
            if (StringValidation.isLengthGreaterThan(vo.getName(), 255))
                response.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
        }
        
        //Verify that they each exist as we will create a mapping when we save
        if(vo.getAddedEventHandlers() != null)
            for(AbstractEventHandlerVO<?> eh : vo.getAddedEventHandlers()) {
                if(EventHandlerDao.getInstance().getXidById(eh.getId()) == null)
                    response.addMessage("handlers", new TranslatableMessage("emport.eventHandler.missing", eh.getXid()));
            }
        if(vo.getEventHandlerXids() != null) {
            for(String ehXid : vo.getEventHandlerXids()) {
                if(EventHandlerDao.getInstance().getIdByXid(ehXid) == null)
                    response.addMessage("eventHandlerXids", new TranslatableMessage("emport.eventHandler.missing", ehXid));
            }
        }
        return response;
    }
}
