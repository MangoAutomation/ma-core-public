/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.springframework.transaction.PlatformTransactionManager;

import com.google.common.collect.Iterables;
import com.serotonin.db.TransactionCapable;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.TranslatableMessageParseException;

public abstract class BaseDao implements TransactionCapable {

    private static final String Y = "Y";
    private static final String N = "N";

    protected final DatabaseProxy databaseProxy;
    protected final DataSource dataSource;
    protected final PlatformTransactionManager transactionManager;
    protected final ExtendedJdbcTemplate ejt;
    protected final DatabaseType databaseType;
    protected final DSLContext create;

    // Print out times and SQL for RQL Queries
    protected final boolean useMetrics;
    protected final long metricsThreshold;

    public BaseDao(DatabaseProxy databaseProxy) {
        this.databaseProxy = databaseProxy;
        this.dataSource = databaseProxy.getDataSource();
        this.transactionManager = databaseProxy.getTransactionManager();
        this.databaseType = databaseProxy.getType();
        this.useMetrics = databaseProxy.isUseMetrics();
        this.metricsThreshold = databaseProxy.metricsThreshold();
        this.ejt = databaseProxy.getJdbcTemplate();
        this.create = databaseProxy.getContext();
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

    /**
     * Bad practice, should be using prepared statements. This is being used to do WHERE x IN(a,b,c)
     */
    @Deprecated
    protected String createDelimitedList(Collection<?> values, String delimiter, String quote) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> iterator = values.iterator();
        boolean first = true;
        while (iterator.hasNext()) {
            if (first)
                first = false;
            else
                sb.append(delimiter);

            if (quote != null)
                sb.append(quote);

            sb.append(iterator.next());

            if (quote != null)
                sb.append(quote);
        }
        return sb.toString();
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

}
