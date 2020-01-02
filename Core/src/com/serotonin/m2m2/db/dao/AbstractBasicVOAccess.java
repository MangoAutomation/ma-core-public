/**
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.vo.AbstractBasicVO;

import net.jazdw.rql.parser.ASTNode;

/**
 * Interface to outline the DAO access methods for Basic VOs and aid in mocks for testing.
 * 
 * @author Terry Packer
 *
 */
public interface AbstractBasicVOAccess<T extends AbstractBasicVO> {

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
     * Return a VO and load its relational data
     *
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
     * Create a custom query with callback for each row
     * @param conditions
     * @param callback
     */
    public void customizedQuery(ConditionSortLimit conditions, MappedRowCallback<T> callback);
    
    /**
     * Create a ConditionSortLimit configuration from an RQL AST Node
     * @param rql
     * @return
     */
    public ConditionSortLimit rqlToCondition(ASTNode rql);
}
