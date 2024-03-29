/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.springframework.core.env.Environment;

import com.infiniteautomation.mango.async.ConcurrentProcessor;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
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
 * @author Terry Packer
 */
public abstract class AbstractBasicVOService<T extends AbstractBasicVO, DAO extends AbstractBasicVOAccess<T>> {

    protected final DAO dao;
    protected final PermissionService permissionService;
    protected final Environment env;
    protected final ExecutorService executorService;
    protected final ConcurrentProcessor<BasicAsyncOperation<T>, T> concurrentProcessor;

    /**
     * Service
     * @param dependencies dependencies for service
     */
    public AbstractBasicVOService(DAO dao, ServiceDependencies dependencies) {
        this.dao = dao;
        this.permissionService = dependencies.getPermissionService();
        this.env = dependencies.getEnvironment();
        this.executorService = dependencies.getExecutorService();

        int defaultMaxConcurrency = env.getProperty("services.maxConcurrency", Integer.class, 10);
        int maxConcurrency = env.getProperty("services." + getClass().getSimpleName() + ".maxConcurrency", Integer.class, defaultMaxConcurrency);
        this.concurrentProcessor = new ConcurrentProcessor<>(this::doAsyncOperation, maxConcurrency, executorService);
    }

    /**
     * Validate a new VO
     */
    abstract public ProcessResult validate(T vo);

    /**
     * Can this user edit this VO
     *
     */
    abstract public boolean hasEditPermission(PermissionHolder user, T vo);

    /**
     * Can this user read this VO
     *
     */
    abstract public boolean hasReadPermission(PermissionHolder user, T vo);


    /**
     * Get the create permission if defined
     *  override as necessary
     */
    protected PermissionDefinition getCreatePermission() {
        return null;
    }

    /**
     * Ensure this vo is valid compared to the previous one.
     *
     * Override as necessary, most VOs won't need this.
     *
     */
    public ProcessResult validate(T existing, T vo) {
        return validate(vo);
    }

    /**
     * Ensure that this VO is valid.
     * Note: validation will only fail if there are Error level messages in the result
     */
    public void ensureValid(T vo) throws ValidationException {
        ProcessResult result = validate(vo);
        if(!result.isValid())
            throw new ValidationException(result, vo.getClass());
    }

    /**
     * Note: validation will only fail if there are Error level messages in the result
     */
    public void ensureValid(T existing, T vo) throws ValidationException {
        ProcessResult result = validate(existing, vo);
        if(!result.isValid()) {
            throw new ValidationException(result, vo.getClass());
        }
    }

    /**
     *
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
     */
    public T insert(T vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        //Ensure they can create
        ensureCreatePermission(user, vo);

        //Ensure id is not set
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result, vo.getClass());
        }

        ensureValid(vo);
        dao.insert(vo);
        return vo;
    }

    /**
     *
     */
    public T update(int existingId, T vo) throws PermissionException, ValidationException {
        return update(get(existingId), vo);
    }

    /**
     *
     */
    protected T update(T existing, T vo) throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        ensureValid(existing, vo);
        dao.update(existing, vo);
        return vo;
    }

    /**
     *
     */
    public T delete(int id) throws PermissionException, NotFoundException {
        T vo = get(id);
        return delete(vo);
    }

    /**
     *
     */
    protected T delete(T vo) throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();
        ensureDeletePermission(user, vo);
        dao.delete(vo);
        return vo;
    }

    /**
     * Create a ConditionSortLimit configuration and allow supplying extra field mappings for model fields to columns
     *  and value converters to translate the RQL conditions into the values expected from the database.
     *
     * @param subSelectMap - can be null
     * @param fieldMap - can be null
     * @param valueConverters - can be null
     */
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, RQLSubSelectCondition> subSelectMap, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters) {
        return dao.rqlToCondition(rql, subSelectMap, fieldMap, valueConverters);
    }

    /**
     * Query for VOs with a callback for each row, filtering within the database is supported
     * by the conditions input, if DAO does not use database filtering you must manually filter on permissions
     * @param conditions SQL conditions to restrict results, with additional sorting, limit and offset
     * @param callback called for every result returned
     */
    public void customizedQuery(ConditionSortLimit conditions, Consumer<T> callback) {
        PermissionHolder user = Common.getUser();
        dao.customizedQuery(conditions, user, (item) ->{
            dao.loadRelationalData(item);
            callback.accept(item);
        });
    }

    /**
     * Query for VOs with a callback for each row, filtering within the database is supported
     * by the conditions input, if DAO does not use database filtering you must manually filter on permissions
     *
     * @param condition SQL conditions to restrict results
     * @param callback called for every result returned
     */
    public void customizedQuery(Condition condition, Consumer<T> callback) {
        customizedQuery(new ConditionSortLimit(condition, null, null, null), callback);
    }

    /**
     * Query for VOs, filtering within the database is supported
     * by the conditions input, if DAO does not use database filtering you must manually filter on permissions
     *
     * @param condition SQL conditions to restrict results
     * @return list of results
     */
    public List<T> customizedQuery(Condition condition) {
        List<T> list = new ArrayList<>();
        customizedQuery(new ConditionSortLimit(condition, null, null, null), list::add);
        return list;
    }

    /**
     * Query for VOs using RQL.  Permissions are filtered within the database if supported by the dao, if not
     * you must filter manually.
     */
    public void customizedQuery(ASTNode conditions, Consumer<T> callback) {
        customizedQuery(dao.rqlToCondition(conditions, null, null, null), callback);
    }

    /**
     * Count VOs
     */
    public int customizedCount(ConditionSortLimit conditions) {
        PermissionHolder user = Common.getUser();
        return dao.customizedCount(conditions, user);
    }

    /**
     * Count VOs
     * @param conditions - RQL AST Node
     */
    public int customizedCount(ASTNode conditions) {
        return customizedCount(dao.rqlToCondition(conditions, null, null, null));
    }

    /**
     * Can this user create this VO
     *
     * @param vo to insert
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
     */
    public boolean hasDeletePermission(PermissionHolder user, T vo) {
        return hasEditPermission(user, vo);
    }

    /**
     * Ensure this user can create this vo
     *
     */
    public void ensureCreatePermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasCreatePermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
    }

    /**
     * Ensure this user can edit this vo
     *
     */
    public void ensureEditPermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasEditPermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
    }

    /**
     * Ensure this user can read this vo
     *
     */
    public void ensureReadPermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasReadPermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
    }

    /**
     * Ensure this user can delete this vo
     */
    public void ensureDeletePermission(PermissionHolder user, T vo) throws PermissionException {
        if(!hasDeletePermission(user, vo))
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user != null ? user.getPermissionHolderName() : null), user);
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

    protected static class BasicAsyncOperation<T> {
        final DaoEventType type;
        final T vo;
        final int id;

        protected BasicAsyncOperation(DaoEventType type, int id) {
            this(type, id, null);
        }

        protected BasicAsyncOperation(DaoEventType type, int id, T vo) {
            this.type = type;
            this.vo = vo;
            this.id = id;
        }
    }

    protected T doAsyncOperation(BasicAsyncOperation<T> operation) {
        switch (operation.type) {
            case GET:
                return get(operation.id);
            case CREATE:
                return insert(operation.vo);
            case UPDATE:
                return update(operation.id, operation.vo);
            case DELETE:
                return delete(operation.id);
            default:
                throw new IllegalStateException("Unknown operation " + operation.type);
        }
    }

    public CompletableFuture<T> getAsync(int id) {
        return concurrentProcessor.add(new BasicAsyncOperation<>(DaoEventType.GET, id));
    }

    public CompletableFuture<T> insertAsync(T vo) {
        return concurrentProcessor.add(new BasicAsyncOperation<>(DaoEventType.CREATE, Common.NEW_ID, vo));
    }

    public CompletableFuture<T> deleteAsync(int id) {
        return concurrentProcessor.add(new BasicAsyncOperation<>(DaoEventType.DELETE, id));
    }

    public CompletableFuture<T> updateAsync(int id, T vo) {
        return concurrentProcessor.add(new BasicAsyncOperation<>(DaoEventType.UPDATE, id, vo));
    }
}
