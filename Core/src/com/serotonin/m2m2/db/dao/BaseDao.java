/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Iterables;
import com.serotonin.db.DaoUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.TranslatableMessageParseException;

public class BaseDao extends DaoUtils {

    public static final String Y = "Y";
    public static final String N = "N";

    /**
     * Public constructor for code that needs to get stuff from the database.
     */
    public BaseDao() {
        super(Common.databaseProxy.getDataSource(), Common.databaseProxy.getTransactionManager());
    }

    //
    // Convenience methods for storage of booleans.
    //
    public static String boolToChar(boolean b) {
        return b ? Y : N;
    }

    public static boolean charToBool(String s) {
        return Y.equals(s);
    }

    //
    // XID convenience methods
    //
    protected String generateUniqueXid(String prefix, String tableName) {
        return Common.generateXid(prefix);
    }

    protected boolean isXidUnique(String xid, int excludeId, String tableName) {
        return ejt.queryForInt("select count(*) from " + tableName + " where xid=? and id<>?", new Object[] { xid,
                excludeId }, 0) == 0;
    }

    /**
     * Get the defined batch size for IN() conditions
     * @return
     */
    protected int getInBatchSize() {
        return Common.envProps.getInt("db.in.maxOperands", 1000);
    }

    /**
     * Break an interable collection into batches to execute large 'in' queries
     * @param <T>
     * @param parameters
     * @return
     */
    protected <T> Iterable<List<T>> batchInParameters(Iterable<T> parameters){
        return Iterables.partition(parameters, getInBatchSize());
    }

    //
    // Convenience methods for translatable messages
    //
    public static String writeTranslatableMessage(TranslatableMessage tm) {
        if (tm == null)
            return null;
        return tm.serialize();
    }

    public static TranslatableMessage readTranslatableMessage(ResultSet rs, int columnIndex) throws SQLException {
        String columnValue = rs.getString(columnIndex);
        return readTranslatableMessage(columnValue);
    }

    public static TranslatableMessage readTranslatableMessage(String columnValue) {
        if (columnValue == null)
            return null;

        try {
            return TranslatableMessage.deserialize(columnValue);
        } catch (TranslatableMessageParseException e) {
            return new TranslatableMessage("common.default", columnValue);
        }
    }
}
