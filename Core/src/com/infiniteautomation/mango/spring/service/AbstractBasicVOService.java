/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.SelectSelectStep;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.spring.db.AbstractBasicTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractBasicVOAccess;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

import net.jazdw.rql.parser.ASTNode;

/**
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
    abstract public  boolean hasEditPermission(PermissionHolder user, T vo);

    /**
     * Can this user read this VO
     *
     * @param user
     * @param vo
     * @return
     */
    abstract public boolean hasReadPermission(PermissionHolder user, T vo);


    /**
     * Get any create permission roles
     *  override as necessary
     * @return
     */
    public Set<Role> getCreatePermissionRoles() {
        return Collections.emptySet();
    }

    /**
     * Handle when a role was deleted with the existing mappings at the time of deletion
     * You must annotate the overridden method with @EventListener in order for this to work.
     * @param event
     */
    protected void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {

    }

    /**
     * A new role was created.
     *  Override as required
     * @param role
     */
    protected void roleCreated(RoleVO role) {

    }

    /**
     * A role was deleted, update any runtime members that have this role.
     *   The database will delete on cascade from the mapping table so this
     *   only concerns runtime data.
     * @param role
     */
    protected void roleDeleted(RoleVO role) {

    }

    /**
     * A role was updated, only the name can be updated.
     *  Override as required
     * @param role
     */
    protected void roleUpdated(RoleVO role) {

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
        return validate(vo, user);
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
            throw new ValidationException(result, vo.getClass());
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
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        ensureReadPermission(user, vo);
        return vo;
    }

    /**
     * Get a list of all items pruned based on the
     * read permission using security context user
     * @return
     */
    public List<T> getAll() {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        List<T> all = dao.getAll();
        Iterator<T> it = all.iterator();
        //Filter list based on permission
        while(it.hasNext()) {
            T vo = it.next();
            if(!hasReadPermission(user, vo)) {
                it.remove();
            }
        }
        return all;
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
        Objects.requireNonNull(user, "Permission holder must be set in security context");

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
    public T update(T existing, T vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

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
    public T delete(T vo) throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        ensureDeletePermission(user, vo);
        dao.delete(vo.getId());
        return vo;
    }

    /**
     * Create a ConditionSortLimit configuration and allow supplying extra field mappings for model fields to columns
     *  and value converters to translate the RQL conditions into the values expected from the database.  Security context user
     *  is used to enforce read permission for the items
     *
     * @param rql
     * @param fieldMap - can be null
     * @param valueConverters - can be null
     * @return
     */
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters) {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        return dao.rqlToCondition(rql, fieldMap, valueConverters, user);
    }

    /**
     * Create a ConditionSortLimit configuration and allow supplying extra field mappings for model fields to columns
     *  and value converters to translate the RQL conditions into the values expected from the database.  Security context user
     *  is used to enforce supplied type of permission for the items
     *
     *
     * @param rql
     * @param fieldMap - can be null
     * @param valueConverters - can be null
     * @param permissionType
     * @return
     */
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters, String permissionType) {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        return dao.rqlToCondition(rql, fieldMap, valueConverters, user, permissionType);
    }

    /**
     * Query for VOs with a callback for each row
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ConditionSortLimit conditions, MappedRowCallback<T> callback) {
        dao.customizedQuery(conditions, (item, index) ->{
            dao.loadRelationalData(item);
            callback.row(item, index);
        });
    }

    /**
     * Query for VOs using RQL
     * @param conditions
     * @param full - load relational data
     * @param callback
     */
    public void customizedQuery(ASTNode conditions, MappedRowCallback<T> callback) {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        dao.customizedQuery(dao.rqlToCondition(conditions, null, null, user), (item, index) ->{
            dao.loadRelationalData(item);
            callback.row(item, index);
        });
    }

    /**
     * Execute arbitrary queries on the database
     * @param select
     * @param callback
     */
    public void customizedQuery(Select<Record> select, ResultSetExtractor<Void> callback) {
        this.dao.customizedQuery(select, callback);
    }

    /**
     * Execute custom query for VOs with a callback per row
     * @param select
     * @param callback
     */
    public void customizedQuery(Select<Record> select, MappedRowCallback<T> callback) {
        dao.customizedQuery(select, (item, index) ->{
            dao.loadRelationalData(item);
            callback.row(item, index);
        });
    }

    /**
     * Get all matching items that calling use has read permission for
     * @param conditions
     * @param callback
     */
    public void customizedQuery(Condition conditions, MappedRowCallback<T> callback) {
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        dao.customizedQuery(conditions, (vo, index) -> {
            if(hasReadPermission(user, vo)) {
                callback.row(vo, index);
            }
        });
    }

    /**
     * Join default tables for DAO
     *
     * @param <R>
     * @param select
     * @param conditions
     * @return
     */
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        return this.dao.joinTables(select, conditions);
    }

    /**
     * Get the select query for the supplied fields without any joins
     * @return
     */
    public SelectJoinStep<Record> getSelectQuery(Field<?>... fields) {
        return dao.getSelectQuery(Arrays.asList(fields));
    }

    /**
     * Get a select query to return full VOs using a row callback
     * @return
     */
    public SelectJoinStep<Record> getJoinedSelectQuery() {
        return dao.getJoinedSelectQuery();
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
        PermissionHolder user = Common.getUser();
        Objects.requireNonNull(user, "Permission holder must be set in security context");

        return dao.customizedCount(dao.rqlToCondition(conditions, null, null, user));
    }

    /**
     * Get the count query for this table
     * @return
     */
    public SelectSelectStep<Record1<Integer>> getCountQuery() {
        return dao.getCountQuery();
    }

    /**
     * Can this user create this VO
     *
     * @param user
     * @param vo to insert
     * @return
     */
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        return permissionService.hasAnyRole(user, getCreatePermissionRoles());
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

    /**
     * Get the DAO
     * @return
     */
    public DAO getDao() {
        return dao;
    }

    /**
     * get the permission service that was wired in
     * @return
     */
    public PermissionService getPermissionService() {
        return permissionService;
    }

}
