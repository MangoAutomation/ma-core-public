/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Validator;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.rest.validation.ProcessMessageContraintViolation;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import net.jazdw.rql.parser.ASTNode;

/**
 * Base Service
 * 
 * @author Terry Packer
 *
 */
public abstract class AbstractVOService<T extends AbstractVO<?>> {
    
    protected final ThreadLocal<Map<String,String>> validationPropertyMap = new ThreadLocal<>();
    protected final AbstractDao<T> dao;
    protected final Validator validator;
    
    public AbstractVOService(AbstractDao<T> dao, Validator validator) {
        this.dao = dao;
        this.validator = validator;
    }
    
    private static final String INDEX_OPEN = "[";
    private static final String INDEX_CLOSE = "]";
    
    /**
     * Validate a VO
     * @param vo
     * @param user
     * @return
     */
    public ProcessResult validate(T vo, User user) {
        ProcessResult result = new ProcessResult();
        Map<String, String> propertyMap = this.validationPropertyMap.get();
        Set<ConstraintViolation<T>> violations = validator.validate(vo);
        for(ConstraintViolation<T> violation : violations) {
            if(violation instanceof ProcessMessageContraintViolation)
                result.addMessage(((ProcessMessageContraintViolation<?>)violation).getProcessMessage());
            else {
                //TODO Support Paths in the validation map
                StringBuilder builder = new StringBuilder();
                violation.getPropertyPath().forEach(node -> {
                    if(node.getKind() != ElementKind.CONTAINER_ELEMENT) {
                        if(!StringUtils.isEmpty(builder))
                            builder.append(".");
                        String mapped = propertyMap.get(node.getName());
                        if(mapped != null)
                            builder.append(mapped);
                        else if(node.getName() != null)
                            builder.append(node.getName());
                        if(node.isInIterable()) {
                            builder.append( INDEX_OPEN );
                            if ( node.getIndex() != null ) {
                                builder.append( node.getIndex() );
                            }
                            else if ( node.getKey() != null ) {
                                builder.append( node.getKey() );
                            }
                            builder.append( INDEX_CLOSE );
                        }

                    }   
                });
                result.addContextualMessage(builder.toString(), violation.getMessage());
            }
        }
        
        //Map the results
        if(propertyMap != null) {
            for(ProcessMessage m : result.getMessages()) {
                String mapped = propertyMap.get(m.getContextKey());
                if(mapped != null) {
                    m.setContextKey(mapped);
                }
            }
        }
        return result;
    }
    
    /**
     * Ensure that this VO is valid.
     * Note: validation will only fail if there are Error level messages in the result
     * @param vo
     * @param user
     */
    public void ensureValid(T vo, User user) throws ValidationException {
        ProcessResult result = validate(vo, user);
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
        return get(xid, user, false);
    }
    
    /**
     * Get relational data too
     * @param xid
     * @param user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     * @throws ValidationException
     */
    public T getFull(String xid, PermissionHolder user) throws NotFoundException, PermissionException, ValidationException {
        return get(xid, user, true);
    }
    
    /**
     * 
     * @param xid
     * @param user
     * @param full
     * @return
     */
    protected T get(String xid, PermissionHolder user, boolean full) {
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
     * Insert a vo with its relational data
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T insertFull(T vo, User user) throws PermissionException, ValidationException {
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
    public T insert(T vo, User user) throws PermissionException, ValidationException {
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
    protected T insert(T vo, User user, boolean full) throws PermissionException, ValidationException {
        //Ensure they can create a list
        ensureCreatePermission(user);
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());
        
        ensureValid(vo, user);
        if(full)
            dao.saveFull(vo);
        else
            dao.save(vo);
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
    public T update(String existingXid, T vo, User user) throws PermissionException, ValidationException {
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
    public T update(T existing, T vo, User user) throws PermissionException, ValidationException {
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
    public T updateFull(String existingXid, T vo, User user) throws PermissionException, ValidationException {
        return updateFull(get(existingXid, user), vo, user);
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
    public T updateFull(T existing, T vo, User user) throws PermissionException, ValidationException {
        return update(existing, vo, user, true);
    }
    
    protected T update(T existing, T vo, User user, boolean full) throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        ensureValid(vo, user);
        if(full)
            dao.saveFull(vo);
        else
            dao.save(vo);
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
    public void ensureCreatePermission(PermissionHolder user) throws PermissionException {
        if(!hasCreatePermission(user))
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

    
    public void setValidationPropertyMap(Map<String, String> map) {
        this.validationPropertyMap.set(map);
    }
    
    public void removeValidationPropertyMap() {
        this.validationPropertyMap.remove();
    }
}
