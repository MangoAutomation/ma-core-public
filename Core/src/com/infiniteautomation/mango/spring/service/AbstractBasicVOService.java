/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.Field;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.spring.db.AbstractBasicTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractBasicVOAccess;
import com.serotonin.m2m2.db.dao.QueryBuilder;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import net.jazdw.rql.parser.ASTNode;

/**
 * TODO Mango 4.0 extract interface
 * @author Terry Packer
 *
 */
public abstract class AbstractBasicVOService<T extends AbstractBasicVO, TABLE extends AbstractBasicTableDefinition, DAO extends AbstractBasicVOAccess<T, TABLE>> {

    protected final DAO dao;
    protected final PermissionService permissionService;

    /**
     * Service
     * @param dao
     * @param permissionService
     * @param createPermissionDefinition
     */
    public AbstractBasicVOService(DAO dao, PermissionService permissionService) {
        this.dao = dao;
        this.permissionService = permissionService;
    }

    /**
     * Validate a new VO
     * @param vo
     * @param user
     * @return
     */
    abstract public ProcessResult validate(T vo, PermissionHolder user);

    /**
     * Can this user edit this VO
     *
     * @param user
     * @param vo
     * @return
     */
    abstract public boolean hasEditPermission(PermissionHolder user, T vo);

    /**
     * Can this user read this VO
     *
     * @param user
     * @param vo
     * @return
     */
    abstract public boolean hasReadPermission(PermissionHolder user, T vo);


    /**
     * Get the create permission if defined
     *  override as necessary
     * @return
     */
    protected PermissionDefinition getCreatePermission() {
        return null;
    }

    /**
     * TODO Mango 4.0 remove user from parameters
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
        return validate(vo, user);
    }

    /**
     * TODO Mango 4.0 remove user from parameters
     *
     * Ensure that this VO is valid.
     * Note: validation will only fail if there are Error level messages in the result
     * @param vo
     * @param user
     */
    public void ensureValid(T vo, PermissionHolder user) throws ValidationException {
        ProcessResult result = validate(vo, user);
        if(!result.isValid())
            throw new ValidationException(result, vo.getClass());
    }

    /**
     * TODO Mango 4.0 remove user from parameters
     * Note: validation will only fail if there are Error level messages in the result
     * @param existing
     * @param vo
     * @param user
     * @throws ValidationException
     */
    public void ensureValid(T existing, T vo, PermissionHolder user) throws ValidationException {
        ProcessResult result = validate(existing, vo, user);
        if(!result.isValid()) {
            throw new ValidationException(result, vo.getClass());
        }
    }

    /**
     *
     * @param id
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public T get(int id) throws NotFoundException, PermissionException {
        T vo = dao.get(id);
        if(vo == null)
            throw new NotFoundException();

        PermissionHolder user = Common.getUser();
        ensureReadPermission(user, vo);
        return vo;
    }

    /**
     *
     * @param vo
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T insert(T vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        //Ensure they can create
        ensureCreatePermission(user, vo);

        //Ensure id is not set
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }

        ensureValid(vo, user);
        dao.insert(vo);
        return vo;
    }

    /**
     *
     * @param existingId
     * @param vo
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public T update(int existingId, T vo) throws PermissionException, ValidationException {
        return update(get(existingId), vo);
    }

    /**
     *
     * @param existing
     * @param vo
     *
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    protected T update(T existing, T vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        ensureValid(existing, vo, user);
        dao.update(existing, vo);
        return vo;
    }

    /**
     *
     * @param id
     * @return
     * @throws PermissionException
     * @throws NotFoundException
     */
    public T delete(int id) throws PermissionException, NotFoundException {
        T vo = get(id);
        return delete(vo);
    }

    /**
     *
     * @param vo
     * @return
     * @throws PermissionException
     * @throws NotFoundException
     */
    protected T delete(T vo) throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();
        ensureDeletePermission(user, vo);
        dao.delete(vo.getId());
        return vo;
    }

    /**
     * Create a ConditionSortLimit configuration and allow supplying extra field mappings for model fields to columns
     *  and value converters to translate the RQL conditions into the values expected from the database.
     *
     * @param rql
     * @param subSelectMap - can be null
     * @param fieldMap - can be null
     * @param valueConverters - can be null
     * @return
     */
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, RQLSubSelectCondition> subSelectMap, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters) {
        return dao.rqlToCondition(rql, subSelectMap, fieldMap, valueConverters);
    }

    /**
     * Query for VOs with a callback for each row, filtering within the database is supported
     * by the conditions input, if dao does not using database filtering you must manually filter on permissions
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ConditionSortLimit conditions, MappedRowCallback<T> callback) {
        PermissionHolder user = Common.getUser();
        dao.customizedQuery(conditions, user, (item, index) ->{
            dao.loadRelationalData(item);
            callback.row(item, index);
        });
    }

    /**
     * Query for VOs using RQL.  Permissions are filtered within the database if supported by the dao, if not
     * you must filter manually.
     * @param conditions
     * @param full - load relational data
     * @param callback
     */
    public void customizedQuery(ASTNode conditions, MappedRowCallback<T> callback) {
        PermissionHolder user = Common.getUser();
        dao.customizedQuery(dao.rqlToCondition(conditions, null, null, null), user, (item, index) ->{
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
        PermissionHolder user = Common.getUser();
        return dao.customizedCount(conditions, user);
    }

    /**
     * Count VOs
     * @param conditions - RQL AST Node
     * @return
     */
    public int customizedCount(ASTNode conditions) {
        PermissionHolder user = Common.getUser();
        return dao.customizedCount(dao.rqlToCondition(conditions, null, null, null), user);
    }

    /**
     * Can this user create this VO
     *
     * @param user
     * @param vo to insert
     * @return
     */
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        PermissionDefinition create = getCreatePermission();
        if(create == null) {
            return permissionService.hasAdminRole(user);
        }else {
            return permissionService.hasPermission(user, create.getPermission());
        }
    }

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
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
    }

    /**
     * Ensure this user can edit this vo
     *
     * @param user
     * @param vo
     */
    public void ensureEditPermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasEditPermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
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
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
    }

    /**
     * Ensure this user can delete this vo
     * @param user
     * @param vo
     * @throws PermissionException
     */
    public void ensureDeletePermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasDeletePermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
    }

    /**
     * get the permission service that was wired in
     * @return
     */
    public PermissionService getPermissionService() {
        return permissionService;
    }

    public int count() {
        return dao.count(Common.getUser());
    }

    public int count(String rql) {
        return dao.count(Common.getUser(), rql);
    }

    public List<T> list() {
        return dao.list(Common.getUser());
    }

    public void list(Consumer<T> consumer) {
        dao.list(Common.getUser(), consumer);
    }

    public List<T> query(String rql) {
        return dao.query(Common.getUser(), rql);
    }

    public void query(String rql, Consumer<T> consumer) {
        dao.query(Common.getUser(), rql, consumer);
    }

    public QueryBuilder<T> buildQuery() {
        return dao.buildQuery(Common.getUser());
    }
}
