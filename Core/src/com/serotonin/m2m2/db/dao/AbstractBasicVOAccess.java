/**
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.jooq.SelectSelectStep;
import org.jooq.SortField;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.spring.db.AbstractBasicTableDefinition;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.db.TransactionCapable;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import net.jazdw.rql.parser.ASTNode;

/**
 * TODO Mango 4.0 rename to focus on Dao
 * Interface to outline the DAO access methods for Basic VOs and aid in mocks for testing.
 *
 * @author Terry Packer
 *
 */
public interface AbstractBasicVOAccess<T extends AbstractBasicVO, TABLE extends AbstractBasicTableDefinition> extends TransactionCapable {

    /**
     * Insert a vo and save relational data in a transaction
     *
     * @param vo
     */
    public void insert(T vo);

    /**
     * Save any data and set FKs that are required prior to saving this VO
     * @param existing (null on inserts)
     * @param vo
     */
    public void savePreRelationalData(T existing, T vo);

    /**
     * Save relational data for a vo to a different table,
     *  this is called within a transaction during insert/update.
     * @param existing (null on inserts)
     * @param vo
     */
    public void saveRelationalData(T existing, T vo);

    /**
     * Update a vo and save its relational data in a transaction.
     * Fire Dao event upon completion
     * @param id existing id of vo
     * @param vo
     */
    public void update(int id, T vo);

    /**
     * Update a vo and save its relational data in a transaction.
     * Fire Dao event upon completion
     * @param existing
     * @param vo
     */
    public void update(T existing, T vo);

    /**
     * Return a VO and load its relational data using the user to ensure read permission
     * @param id
     * @return
     */
    public T get(int id);

    /**
     * Callback for all VOs with FKs Populated optionally
     *
     * @return
     */
    public void getAll(MappedRowCallback<T> callback);

    /**
     * Return all VOs with FKs Populated optionally
     *
     * @return
     */
    public List<T> getAll();

    /**
     * Load relational data from another table, does not happen within a transaction
     * @param vo
     */
    public void loadRelationalData(T vo);

    /**
     * Delete a VO based on its id this will always get the FKs to ensure they will be deleted if
     * there is no ON CASCADE for the FK
     *
     * @param id
     * @return true if was deleted
     */
    public boolean delete(int id);

    /**
     * Delete a VO (uses id to find it)
     * @param vo
     * @return true if was deleted
     */
    public boolean delete(T vo);

    /**
     * Optionally delete any relational data, this is called in a transaction
     *  during deletion.
     *
     * @param vo
     */
    public void deleteRelationalData(T vo);

    /**
     * Optionally perform any logic after the VO is deleted
     * @param vo
     */
    public void deletePostRelationalData(T vo);

    /**
     * Count all from table
     *
     * @return
     */
    public int count();

    /**
     * Create a custom count query
     * @param conditions
     * @return
     */
    public int customizedCount(ConditionSortLimit conditions, PermissionHolder user);

    /**
     * Get the default count query
     * @return
     */
    public SelectSelectStep<Record1<Integer>> getCountQuery();

    /**
     * TODO Mango 4.0 is the condition necessary?  Change to accept different query step
     * Create a custom count query
     * @param input
     * @param condition
     * @return
     */
    public int customizedCount(SelectJoinStep<Record1<Integer>> input, Condition condition);

    /**
     * TODO Mango 4.0 Remove this
     * Create a custom query with callback for each row
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ConditionSortLimit conditions, PermissionHolder user, MappedRowCallback<T> callback);

    /**
     * Adapts the callback to a consumer.
     *
     * @param conditions
     * @param user
     * @param consumer
     */
    public default void customizedQuery(ConditionSortLimit conditions, PermissionHolder user, Consumer<T> consumer) {
        customizedQuery(conditions, user, (item, i) -> consumer.accept(item));
    }

    /**
     * Execute a query for VOs with a callback per row
     * @param select
     * @param callback
     */
    public void customizedQuery(Select<Record> select, MappedRowCallback<T> callback);

    /**
     * Execute a custom query and extract results
     * @param select
     * @param callback
     */
    public <TYPE> TYPE customizedQuery(Select<Record> select, ResultSetExtractor<TYPE> callback);

    /**
     * Explicit convenience method for verbose custom queries
     *
     * @param select
     * @param condition
     * @param sort
     * @param limit
     * @param offset
     * @param callback
     */
    public void customizedQuery(SelectJoinStep<Record> select,
            Condition condition, List<SortField<Object>> sort,
            Integer limit, Integer offset,
            MappedRowCallback<T> callback);

    /**
     * Get the select query for the supplied fields without any joins
     * @param fields
     * @return
     */
    public SelectJoinStep<Record> getSelectQuery(List<Field<?>> fields);

    /**
     * Get the select query with default joins
     * @return
     */
    public SelectJoinStep<Record> getJoinedSelectQuery();

    /**
     * Join default tables for DAO
     *
     * @param <R>
     * @param select
     * @param conditions
     * @return
     */
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions);

    /**
     * Join the permissions table to restrict to viewable records
     * @param <R>
     * @param select
     * @param conditions
     * @param user
     * @return
     */
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions, PermissionHolder user);

    /**
     * Get the table model
     * @return
     */
    public TABLE getTable();

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
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, RQLSubSelectCondition> subSelectMap, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters);

    /**
     * Issues a SELECT FOR UPDATE for the row with the given id. Enables transactional updates on rows.
     * @param id
     */
    public void lockRow(int id);

    public default void withLockedRow(int id, Consumer<TransactionStatus> callback) {
        doInTransaction(txStatus -> {
            lockRow(id);
            callback.accept(txStatus);
        });
    }

    public default <X> X withLockedRow(int id, TransactionCallback<X> callback) {
        return doInTransaction(txStatus -> {
            lockRow(id);
            return callback.doInTransaction(txStatus);
        });
    }

    public int count(PermissionHolder user);

    public int count(PermissionHolder user, String rql);

    public List<T> list(PermissionHolder user);

    public void list(PermissionHolder user, Consumer<T> consumer);

    public List<T> query(PermissionHolder user, String rql);

    public void query(PermissionHolder user, String rql, Consumer<T> consumer);

    public QueryBuilder<T> buildQuery(PermissionHolder user);
}
