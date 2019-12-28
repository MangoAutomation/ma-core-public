/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

/**
 * Base Service
 * 
 * @author Terry Packer
 *
 */
public abstract class AbstractVOService<T extends AbstractVO<?>, DAO extends AbstractDao<T>> extends AbstractBasicVOService<T,DAO> {
    

    
    public AbstractVOService(DAO dao, PermissionService permissionService, PermissionDefinition createPermissionDefinition) {
        super(dao, permissionService, createPermissionDefinition);
    }
    
    public AbstractVOService(DAO dao, PermissionService permissionService) {
        this(dao, permissionService, null);
    }
    
    /**
     * Validate a VO
     * @param vo
     * @param user
     * @return
     */
    @Override
    public ProcessResult validate(T vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        if (StringUtils.isBlank(vo.getXid()))
            result.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getXid(), 100))
            result.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
        else if (!isXidUnique(vo.getXid(), vo.getId()))
            result.addContextualMessage("xid", "validate.xidUsed");

        if (StringUtils.isBlank(vo.getName()))
            result.addContextualMessage("name", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getName(), 255))
            result.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
        return result;
    }

    protected boolean isXidUnique(String xid, int id){
        return dao.isXidUnique(xid,id);
    }

    
    /**
     * 
     * @param xid
     * @param user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public T get(String xid, PermissionHolder user) throws NotFoundException, PermissionException {
        return get(xid, user, false);
    }
    
    /**
     * Get relational data too
     * @param xid
     * @param user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public T getFull(String xid, PermissionHolder user) throws NotFoundException, PermissionException {
        return get(xid, user, true);
    }
    
    /**
     * 
     * @param xid
     * @param user
     * @param full
     * @return
     */
    protected T get(String xid, PermissionHolder user, boolean full) throws NotFoundException, PermissionException {
        T vo;
        if(full)
            vo = dao.getFullByXid(xid);
        else
            vo = dao.getByXid(xid);
           
        if(vo == null)
            throw new NotFoundException();
        ensureReadPermission(user, vo);
        return vo;
    }

    /**
     * 
     * @param vo
     * @param user
     * @param full
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    @Override
    protected T insert(T vo, PermissionHolder user, boolean full) throws PermissionException, ValidationException {
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
        dao.insert(vo, full);
        return vo;
    }

    /**
     * Update a vo without its relational data
     * @param existingXid
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T update(String existingXid, T vo, PermissionHolder user) throws PermissionException, ValidationException {
        return update(get(existingXid, user), vo, user);
    }
    
    /**
     * Update a vo and its relational data
     * @param existingXid
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T updateFull(String existingXid, T vo, PermissionHolder user) throws PermissionException, ValidationException {
        return updateFull(getFull(existingXid, user), vo, user);
    }
    
    /**
     * 
     * @param xid
     * @param user
     * @return
     * @throws PermissionException
     */
    public T delete(String xid, PermissionHolder user) throws PermissionException, NotFoundException {
        T vo = get(xid, user, true);
        return delete(vo, user);
    }
    
}
