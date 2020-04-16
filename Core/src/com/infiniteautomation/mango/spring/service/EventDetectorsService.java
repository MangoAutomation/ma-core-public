/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.db.EventDetectorTableDefinition;
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
public class EventDetectorsService extends AbstractVOService<AbstractEventDetectorVO, EventDetectorTableDefinition, EventDetectorDao>{

    @Autowired
    public EventDetectorsService(EventDetectorDao dao, PermissionService permissionService) {
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
    public AbstractEventDetectorVO insertAndReload(AbstractEventDetectorVO vo, boolean restartSource)
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
    public AbstractEventDetectorVO updateAndReload(String existing, AbstractEventDetectorVO vo, boolean restartSource) {
        AbstractEventDetectorVO updated = update(get(existing), vo);
        if(restartSource)
            updated.getDefinition().restartSource(updated);
        return updated;
    }

    @Override
    public AbstractEventDetectorVO delete(AbstractEventDetectorVO vo)
            throws PermissionException, NotFoundException {
        AbstractEventDetectorVO deleted = super.delete(vo);
        vo.getDefinition().restartSource(deleted);
        return vo;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, AbstractEventDetectorVO vo) {
        if(user.hasAdminRole())
            return true;
        return vo.getDefinition().hasCreatePermission(user, vo);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, AbstractEventDetectorVO vo) {
        if(user.hasAdminRole())
            return true;
        if(permissionService.hasPermission(user, vo.getEditPermission()))
            return true;
        return vo.getDefinition().hasEditPermission(user, vo);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, AbstractEventDetectorVO vo) {
        if(user.hasAdminRole())
            return true;
        if(permissionService.hasPermission(user, vo.getReadPermission()))
            return true;
        return vo.getDefinition().hasReadPermission(user, vo);
    }

    @Override
    public ProcessResult validate(AbstractEventDetectorVO vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        permissionService.validateVoRoles(response, "readPermission", user, false, null, vo.getReadPermission());
        permissionService.validateVoRoles(response, "editPermission", user, false, null, vo.getEditPermission());
        vo.getDefinition().validate(response, vo, user);
        return response;
    }

    @Override
    public ProcessResult validate(AbstractEventDetectorVO existing, AbstractEventDetectorVO vo, PermissionHolder user) {
        ProcessResult response = commonValidation(vo, user);
        permissionService.validateVoRoles(response, "readPermission", user, false, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validateVoRoles(response, "editPermission", user, false, existing.getEditPermission(), vo.getEditPermission());

        vo.getDefinition().validate(response, existing, vo, user);
        return response;
    }

    private ProcessResult commonValidation(AbstractEventDetectorVO vo, PermissionHolder user) {
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
            for(AbstractEventHandlerVO eh : vo.getAddedEventHandlers()) {
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
