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
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import net.jazdw.rql.parser.ASTNode;

/**
 * Base Service
 * 
 * @author Terry Packer
 *
 */
public abstract class AbstractVOService<T extends AbstractVO<T>, DAO extends AbstractDao<T>> {
    
    protected final DAO dao;
    
    public AbstractVOService(DAO dao) {
        this.dao = dao;
    }
    
    /**
     * Get the DAO
     * @return
     */
    public DAO getDao() {
        return dao;
    }
    
    /**
     * Validate a VO
     * @param vo
     * @param user
     * @return
     */
    public ProcessResult validate(T vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        vo.validate(result);
        return result;
    }
    
    /**
     * Ensure that this VO is valid.
     * Note: validation will only fail if there are Error level messages in the result
     * @param vo
     * @param user
     */
    public void ensureValid(T vo, PermissionHolder user) throws ValidationException {
        ProcessResult result = validate(vo, user);
        if(!result.isValid())
            throw new ValidationException(result);
    }
    
    /**
     * Ensure this vo is valid compared to the previous one. 
     * 
     * Override as necessary, most VOs won't need this.
     * 
     * @param existing
     * @param vo
     * @param user
     * @return
     */
    public ProcessResult validate(T existing, T vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        vo.validate(result);
        return result;
    }
    
    /**
     * Note: validation will only fail if there are Error level messages in the result
     * @param existing
     * @param vo
     * @param user
     * @throws ValidationException
     */
    public void ensureValid(T existing, T vo, PermissionHolder user) throws ValidationException {
        ProcessResult result = validate(existing, vo, user);
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
     */
    public T get(Integer id, PermissionHolder user) throws NotFoundException, PermissionException {
        return get(id, user, false);
    }
    
    /**
     * 
     * @param id
     * @param user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public T getFull(Integer id, PermissionHolder user) throws NotFoundException, PermissionException {
        return get(id, user, true);
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
     * @param id
     * @param user
     * @param full
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    protected T get(Integer id, PermissionHolder user, boolean full) throws NotFoundException, PermissionException {
        T vo;
        if(full)
            vo = dao.getFull(id);
        else
            vo = dao.get(id);
           
        if(vo == null)
            throw new NotFoundException();
        ensureReadPermission(user, vo);
        return vo;
    }
    
    /**
     * Insert a vo with its relational data
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T insertFull(T vo, PermissionHolder user) throws PermissionException, ValidationException {
        return insert(vo, user, true);
    }
    
    /**
     * Insert a vo without its relational data
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T insert(T vo, PermissionHolder user) throws PermissionException, ValidationException {
        return insert(vo, user, false);
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
    protected T insert(T vo, PermissionHolder user, boolean full) throws PermissionException, ValidationException {
        //Ensure they can create a list
        ensureCreatePermission(user, vo);
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());
        
        ensureValid(vo, user);
        //TODO add initiator id?
        String initiatorId = null;
        dao.insert(vo, initiatorId, full);
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
     * Update a vo without its relational data
     * @param existing
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T update(T existing, T vo, PermissionHolder user) throws PermissionException, ValidationException {
       return update(existing, vo, user, false);
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
     * Update a vo and its relational data
     * @param existing
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T updateFull(T existing, T vo, PermissionHolder user) throws PermissionException, ValidationException {
        return update(existing, vo, user, true);
    }
    
    protected T update(T existing, T vo, PermissionHolder user, boolean full) throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        ensureValid(existing, vo, user);
        //TODO add initiator id?
        String initiatorId = null;
        dao.update(existing, vo, initiatorId, full);
        return vo;
    }

    
    /**
     * 
     * @param xid
     * @param user
     * @return
     * @throws PermissionException
     */
    public T delete(String xid, PermissionHolder user) throws PermissionException, NotFoundException {
        T vo = get(xid, user);
        ensureDeletePermission(user, vo);
        dao.delete(vo.getId());
        return vo;
    }
    
    /**
     * Query for VOs without returning the relational info
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ConditionSortLimit conditions, MappedRowCallback<T> callback) {
        dao.customizedQuery(conditions, callback);
    }
    
    /**
     * Query for VOs and load the relational info
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ASTNode conditions, MappedRowCallback<T> callback) {
        dao.customizedQuery(dao.rqlToCondition(conditions), callback);
    }
    
    /**
     * Query for VOs and load the relational info
     * @param conditions
     * @param callback
     */
    public void customizedQueryFull(ConditionSortLimit conditions, MappedRowCallback<T> callback) {
        dao.customizedQuery(conditions, (item, index) ->{
            dao.loadRelationalData(item);
            callback.row(item, index);
        });
    }
    
    /**
     * Query for VOs and collect the relational info
     * @param conditions
     * @param callback
     */
    public void customizedQueryFull(ASTNode conditions, MappedRowCallback<T> callback) {
        dao.customizedQuery(dao.rqlToCondition(conditions), (item, index) ->{
            dao.loadRelationalData(item);
            callback.row(item, index);
        });
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
     * Can this user create this VO
     * 
     * @param user
     * @param vo to insert
     * @return
     */
    public abstract boolean hasCreatePermission(PermissionHolder user, T vo);
    
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
     * Optionally override to have specific delete permissions, default is same as edit
     * @param user
     * @param vo
     * @return
     */
    public boolean hasDeletePermission(PermissionHolder user, T vo) {
        return hasEditPermission(user, vo);
    }
    
    /**
     * Ensure this user can create this vo
     * 
     * @param user
     * @throws PermissionException
     */
    public void ensureCreatePermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasCreatePermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
    }
    
    /**
     * Ensure this user can edit this vo
     * 
     * @param user
     * @param vo
     */
    public void ensureEditPermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasEditPermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
    }
    
    /**
     * Ensure this user can read this vo
     * 
     * @param user
     * @param vo
     * @throws PermissionException
     */
    public void ensureReadPermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasReadPermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
    }
    
    /**
     * Ensure this user can delete this vo
     * @param user
     * @param vo
     * @throws PermissionException
     */
    public void ensureDeletePermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasDeletePermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
    }
}
