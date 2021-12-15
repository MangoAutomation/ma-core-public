/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
public interface AbstractBasicVOAccess<T extends AbstractBasicVO> extends TransactionCapable {

    /**
     * Insert a vo and save relational data in a transaction
     *
     */
    void insert(T vo);

    /**
     * Save any data and set FKs that are required prior to saving this VO
     * @param existing (null on inserts)
     */
    void savePreRelationalData(T existing, T vo);

    /**
     * Save relational data for a vo to a different table,
     *  this is called within a transaction during insert/update.
     * @param existing (null on inserts)
     */
    void saveRelationalData(T existing, T vo);

    /**
     * Update a vo and save its relational data in a transaction.
     * Fire Dao event upon completion
     * @param id existing id of vo
     */
    void update(int id, T vo);

    /**
     * Update a vo and save its relational data in a transaction.
     * Fire Dao event upon completion
     */
    void update(T existing, T vo);

    /**
     * Return a VO and load its relational data using the user to ensure read permission
     */
    T get(int id);

    /**
     * Callback for all VOs with FKs Populated optionally
     *
     */
    void getAll(Consumer<T> callback);

    /**
     * Return all VOs with FKs Populated optionally
     *
     */
    List<T> getAll();

    /**
     * Load relational data from another table, does not happen within a transaction
     */
    void loadRelationalData(T vo);

    /**
     * Delete a VO based on its id this will always get the FKs to ensure they will be deleted if
     * there is no ON CASCADE for the FK
     *
     * @return true if was deleted
     */
    boolean delete(int id);

    /**
     * Delete a VO (uses id to find it)
     * @return true if was deleted
     */
    boolean delete(T vo);

    /**
     * Optionally delete any relational data, this is called in a transaction
     *  during deletion.
     *
     */
    void deleteRelationalData(T vo);

    /**
     * Optionally perform any logic after the VO is deleted
     */
    void deletePostRelationalData(T vo);

    /**
     * Count all from table
     *
     */
    int count();

    /**
     * Create a custom count query
     */
    int customizedCount(ConditionSortLimit conditions, PermissionHolder user);

    /**
     * Get the default count query
     */
    SelectSelectStep<Record1<Integer>> getCountQuery();

    /**
     * TODO Mango 4.0 is the condition necessary?  Change to accept different query step
     * Create a custom count query
     */
    int customizedCount(SelectJoinStep<Record1<Integer>> input, Condition condition);

    /**
     * Adapts the callback to a consumer.
     *
     */
    void customizedQuery(ConditionSortLimit conditions, PermissionHolder user, Consumer<T> consumer);

    /**
     * Execute a query for VOs with a callback per row
     */
    void customizedQuery(Select<Record> select, Consumer<T> callback);

    /**
     * Execute a custom query and extract results
     */
    <TYPE> TYPE customizedQuery(Select<Record> select, ResultSetExtractor<TYPE> callback);

    /**
     * Explicit convenience method for verbose custom queries
     *
     */
    void customizedQuery(SelectJoinStep<Record> select,
            Condition condition, List<SortField<?>> sort,
            Integer limit, Integer offset,
            Consumer<T> callback);

    /**
     * Get the select query for the supplied fields without any joins
     */
    SelectJoinStep<Record> getSelectQuery(List<Field<?>> fields);

    /**
     * Get the select query with default joins
     */
    SelectJoinStep<Record> getJoinedSelectQuery();

    /**
     * Join default tables for DAO
     *
     */
    <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions);

    /**
     * Join the permissions table to restrict to viewable records
     */
    <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, PermissionHolder user);

    Field<Integer> getReadPermissionField();

    /**
     * Create a ConditionSortLimit configuration and allow supplying extra field mappings for model fields to columns
     *  and value converters to translate the RQL conditions into the values expected from the database.
     *
     * @param subSelectMap - can be null
     * @param fieldMap - can be null
     * @param valueConverters - can be null
     */
    ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, RQLSubSelectCondition> subSelectMap, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters);

    /**
     * Issues a SELECT FOR UPDATE for the row with the given id. Enables transactional updates on rows.
     */
    void lockRow(int id);

    default void withLockedRow(int id, Consumer<TransactionStatus> callback) {
        doInTransaction(txStatus -> {
            lockRow(id);
            callback.accept(txStatus);
        });
    }

    default <X> X withLockedRow(int id, TransactionCallback<X> callback) {
        return doInTransaction(txStatus -> {
            lockRow(id);
            return callback.doInTransaction(txStatus);
        });
    }

    int count(PermissionHolder user);

    int count(PermissionHolder user, String rql);

    List<T> list(PermissionHolder user);

    void list(PermissionHolder user, Consumer<T> consumer);

    List<T> query(PermissionHolder user, String rql);

    void query(PermissionHolder user, String rql, Consumer<T> consumer);

    QueryBuilder<T> buildQuery(PermissionHolder user);
}
