/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

/**
 * Base Service
 * 
 * @author Terry Packer
 *
 */
public abstract class AbstractVOMangoService<T extends AbstractVO<T>> {
    
    protected final AbstractDao<T> dao;
    
    public AbstractVOMangoService(AbstractDao<T> dao) {
        this.dao = dao;
    }
    
    public void ensureValid(T vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        if (StringUtils.isBlank(vo.getXid()))
            result.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getXid(), 100))
            result.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
        else if (!dao.isXidUnique(vo.getXid(), vo.getId()))
            result.addContextualMessage("xid", "validate.xidUsed");

        if (StringUtils.isBlank(vo.getName()))
            result.addContextualMessage("name", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getName(), 255))
            result.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
        
        ensureValidImpl(vo, user, result);
        if(!result.isValid())
            throw new ValidationException(result);
    }
    
    /**
     * Validate the VO, not necessary to throw exception as this will be done in ensureValid()
     * @param vo
     * @param user
     * @param result
     */
    protected abstract void ensureValidImpl(T vo, PermissionHolder user, ProcessResult result);
    

}
