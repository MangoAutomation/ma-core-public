/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;

import org.springframework.transaction.support.TransactionCallback;

import com.infiniteautomation.mango.spring.db.AbstractTableDefinition;
import com.serotonin.db.TransactionCallbackNoResult;
import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Interface to outline the DAO access methods for Abstract VOs and aid in mocks for testing.
 *
 * TODO Mango 4.0 Would really like the Generics to be T extends AbstractVO
 *
 * @author Terry Packer
 *
 */
public interface AbstractVOAccess<T extends AbstractVO, TABLE extends AbstractTableDefinition> extends AbstractBasicVOAccess<T, TABLE> {

    /**
     * Generates a unique XID
     *
     * @return A new unique XID, null if XIDs are not supported
     */
    public String generateUniqueXid();

    /**
     * Checks if a XID is unique
     *
     * @param XID
     *            to check
     * @param excludeId
     * @return True if XID is unique
     */
    public boolean isXidUnique(String xid, int excludeId);

    /**
     * Get the ID for an XID
     * @return Integer
     */
    public Integer getIdByXid(String xid);

    /**
     * Get the ID for an XID
     * @return String
     */
    public String getXidById(int id);

    /**
     * Find a VO by its XID
     *
     * @param xid
     *            XID to search for
     * @return vo if found, otherwise null
     */
    public T getByXid(String xid);


    /**
     * Find VOs by name
     *
     * @param name
     *            name to search for
     * @return List of VO with matching name
     */
    public List<T> getByName(String name);

    /**
     * Issues a SELECT FOR UPDATE for the row with the given xid. Enables transactional updates on rows.
     * @param xid
     */
    public void lockRow(String xid);

    public default void withLockedRow(String xid, TransactionCallbackNoResult callback) {
        doInTransaction(txStatus -> {
            lockRow(xid);
            callback.doInTransactionNoResult(txStatus);
        });
    }

    public default <X> X withLockedRow(String xid, TransactionCallback<X> callback) {
        return doInTransaction(txStatus -> {
            lockRow(xid);
            return callback.doInTransaction(txStatus);
        });
    }
}
