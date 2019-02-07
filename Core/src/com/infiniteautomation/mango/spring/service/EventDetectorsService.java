/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

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

    /**
     * Save a detector an optionally reload its source
     * @param vo
     * @param user
     * @param full
     * @param restartSource
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T insertFull(T vo, PermissionHolder user, boolean restartSource)
            throws PermissionException, ValidationException {
        vo = super.insert(vo, user, true);
        
        if(restartSource)
            vo.getDefinition().restartSource(vo);
        
        return vo;
    }
    
    /**
     * Update and optionally restart the source
     * 
     * @param existing
     * @param vo
     * @param user
     * @param restartSource
     * @return
     */
    public T updateFull(String existing, T vo, PermissionHolder user, boolean restartSource) {
        return updateFull(get(existing, user), vo, user, restartSource);
    }
    
    /**
     * Update and optionally restart the source
     * @param existing
     * @param vo
     * @param user
     * @param full
     * @param restartSource
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T updateFull(T existing, T vo, PermissionHolder user, boolean restartSource) throws PermissionException, ValidationException {
        vo = super.updateFull(existing, vo, user);
        if(restartSource)
            vo.getDefinition().restartSource(vo);
        return vo;
    }
    
    @Override
    public T delete(String xid, PermissionHolder user)
            throws PermissionException, NotFoundException {
        T vo = get(xid, user);
        ensureDeletePermission(user, vo);
        vo.getDefinition().restartSource(vo);
        return vo;
    }
    
    @Autowired
    public EventDetectorsService(EventDetectorDao<T> dao) {
        super(dao);
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        return vo.getDefinition().hasCreatePermission(user, vo);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, T vo) {
        return vo.getDefinition().hasEditPermission(user, vo);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, T vo) {
        return vo.getDefinition().hasReadPermission(user, vo);
    }

}
