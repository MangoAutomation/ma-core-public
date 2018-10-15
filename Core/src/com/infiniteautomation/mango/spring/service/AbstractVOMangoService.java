/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

import net.jazdw.rql.parser.ASTNode;

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
    
    /**
     * 
     * @param vo
     * @param user
     */
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
     * 
     * @param xid
     * @param user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     * @throws ValidationException
     */
    public T get(String xid, PermissionHolder user) throws NotFoundException, PermissionException, ValidationException {
        T vo = dao.getFullByXid(xid);
        if(vo == null)
            throw new NotFoundException();
        ensureReadPermission(user, vo);
        return vo;
    }


    /**
     * 
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T insert(T vo, PermissionHolder user) throws PermissionException, ValidationException {
        //Ensure they can create a list
        ensureCreatePermission(user);
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());
        
        ensureValid(vo, user);
        dao.saveFull(vo);
        return vo;
    }

    /**
     * 
     * @param existingXid
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T update(String existingXid, T vo, User user) throws PermissionException, ValidationException {
        return update(get(existingXid, user), vo, user);
    }


    /**
     * 
     * @param existing
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T update(T existing, T vo, PermissionHolder user) throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        ensureValid(vo, user);
        dao.saveFull(vo);
        return vo;
    }
    
    /**
     * 
     * @param xid
     * @param user
     * @return
     * @throws PermissionException
     */
    public T delete(String xid, PermissionHolder user) throws PermissionException {
        T vo = get(xid, user);
        ensureEditPermission(user, vo);
        dao.delete(vo.getId());
        return vo;
    }
    
    /**
     * Query for VOs
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ConditionSortLimit conditions, MappedRowCallback<T> callback) {
        dao.customizedQuery(conditions, callback);
    }
    
    /**
     * Query for VOs
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ASTNode conditions, MappedRowCallback<T> callback) {
        dao.customizedQuery(dao.rqlToCondition(conditions), callback);
    }
    
    /**
     * Count VOs
     * @param conditions
     * @return
     */
    public int customizedCount(ConditionSortLimit conditions) {
        return dao.customizedCount(conditions);
    }
    
    /**
     * Count VOs
     * @param conditions - RQL AST Node
     * @return
     */
    public int customizedCount(ASTNode conditions) {
        return dao.customizedCount(dao.rqlToCondition(conditions));
    }

    /**
     * Can this user create any VOs
     * 
     * @param user
     * @return
     */
    public abstract boolean hasCreatePermission(PermissionHolder user);
    
    /**
     * Can this user edit this VO
     * 
     * @param user
     * @param vo
     * @return
     */
    public abstract boolean hasEditPermission(PermissionHolder user, T vo);
    
    /**
     * Can this user read this VO
     * 
     * @param user
     * @param vo
     * @return
     */
    public abstract boolean hasReadPermission(PermissionHolder user, T vo);

    /**
     * Ensure this user can create a vo
     * 
     * @param user
     * @throws PermissionException
     */
    public abstract void ensureCreatePermission(PermissionHolder user) throws PermissionException;
    
    /**
     * Ensure this user can edit this vo
     * 
     * @param user
     * @param vo
     */
    public abstract void ensureEditPermission(PermissionHolder user, T vo) throws PermissionException;
    
    /**
     * Ensure this user can read this vo
     * 
     * @param user
     * @param vo
     * @throws PermissionException
     */
    public abstract void ensureReadPermission(PermissionHolder user, T vo) throws PermissionException;
    
    /**
     * Validate the VO, not necessary to throw exception as this will be done in ensureValid()
     * @param vo
     * @param user
     * @param result
     */
    protected abstract void ensureValidImpl(T vo, PermissionHolder user, ProcessResult result);
    

}
