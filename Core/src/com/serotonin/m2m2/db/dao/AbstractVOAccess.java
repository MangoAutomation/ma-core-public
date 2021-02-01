/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.function.Consumer;

import org.jooq.Record;
import org.jooq.Table;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.serotonin.m2m2.vo.AbstractVO;

/**
 * Interface to outline the DAO access methods for Abstract VOs and aid in mocks for testing.
 *
 * TODO Mango 4.0 Would really like the Generics to be T extends AbstractVO
 * TODO Mango 4.0 Rename to something DAO-ish
 *
 * @author Terry Packer
 *
 */
public interface AbstractVOAccess<T extends AbstractVO, R extends Record, TABLE extends Table<R>> extends AbstractBasicVOAccess<T, R, TABLE> {

    /**
     * Generates a unique XID
     *
     * @return A new unique XID, null if XIDs are not supported
     */
    String generateUniqueXid();

    /**
     * Checks if a XID is unique
     *
     * @param xid to check
     * @param excludeId
     * @return True if XID is unique
     */
    boolean isXidUnique(String xid, int excludeId);

    /**
     * Get the ID for an XID
     * @return Integer
     */
    Integer getIdByXid(String xid);

    /**
     * Get the ID for an XID
     * @return String
     */
    String getXidById(int id);

    /**
     * Find a VO by its XID
     *
     * @param xid
     *            XID to search for
     * @return vo if found, otherwise null
     */
    T getByXid(String xid);


    /**
     * Find VOs by name
     *
     * @param name
     *            name to search for
     * @return List of VO with matching name
     */
    List<T> getByName(String name);

    /**
     * Issues a SELECT FOR UPDATE for the row with the given xid. Enables transactional updates on rows.
     * @param xid
     */
    void lockRow(String xid);

    default void withLockedRow(String xid, Consumer<TransactionStatus> callback) {
        doInTransaction(txStatus -> {
            lockRow(xid);
            callback.accept(txStatus);
        });
    }

    default <X> X withLockedRow(String xid, TransactionCallback<X> callback) {
        return doInTransaction(txStatus -> {
            lockRow(xid);
            return callback.doInTransaction(txStatus);
        });
    }
}
