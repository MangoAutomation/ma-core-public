/**
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.Map;
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

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.spring.db.AbstractBasicTableDefinition;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.vo.AbstractBasicVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import net.jazdw.rql.parser.ASTNode;

/**
 * Interface to outline the DAO access methods for Basic VOs and aid in mocks for testing.
 *
 * @author Terry Packer
 *
 */
public interface AbstractBasicVOAccess<T extends AbstractBasicVO, TABLE extends AbstractBasicTableDefinition> {

    /**
     * Insert a vo and save relational data in a transaction
     *
     * @param vo
     */
    public void insert(T vo);

    /**
     * Save relational data for a vo to a different table,
     *  this is called within a transaction during insert/update.
     * @param vo
     * @param insert (true for insert, false for update)
     */
    public void saveRelationalData(T vo, boolean insert);

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
    public int customizedCount(ConditionSortLimit conditions);

    /**
     * Get the default count query
     * @return
     */
    public SelectSelectStep<Record1<Integer>> getCountQuery();

    /**
     * Create a custom count query
     * @param input
     * @param condition
     * @return
     */
    public int customizedCount(SelectJoinStep<Record1<Integer>> input, Condition condition);

    /**
     * Explicit convenience method for verbose custom queries
     * @param select
     * @param condition
     * @param sort
     * @param limit
     * @param offset
     * @param callback
     */
    public void customizedQuery(SelectJoinStep<Record> select, Condition condition, List<SortField<Object>> sort, Integer limit, Integer offset, MappedRowCallback<T> callback);

    /**
     * Create a custom query with callback for each row
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ConditionSortLimit conditions, MappedRowCallback<T> callback);

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
     * Execute a query for these VOs based on the conditions
     *
     * @param conditions
     * @param callback
     */
    public void customizedQuery(Condition conditions, MappedRowCallback<T> callback);

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
     * Get the table model
     * @return
     */
    public TABLE getTable();

    /**
     * Create a ConditionSortLimit configuration and allow supplying extra field mappings for model fields to columns
     *  and value converters to translate the RQL conditions into the values expected from the database.  Supplied user
     *  is used to enforce read permission for the items
     *
     * @param rql
     * @param fieldMap - can be null
     * @param valueConverters - can be null
     * @param user - cannot be null if the item has permissions
     * @return
     */
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters, PermissionHolder user);

    /**
     * Create a ConditionSortLimit configuration and allow supplying extra field mappings for model fields to columns
     *  and value converters to translate the RQL conditions into the values expected from the database.  Supplied user
     *  is used to enforce supplied type of permission for the items
     *
     *
     * @param rql
     * @param fieldMap - can be null
     * @param valueConverters - can be null
     * @param user - cannot be null if the item has permissions
     * @param permissionType
     * @return
     */
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters, PermissionHolder user, String permissionType);

}
