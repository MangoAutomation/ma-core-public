package com.serotonin.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.db.spring.ArgPreparedStatementSetter;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseType;

public class DaoUtils implements TransactionCapable {
    protected final DataSource dataSource;
    protected final PlatformTransactionManager transactionManager;
    protected final ExtendedJdbcTemplate ejt;
    protected DataSourceTransactionManager tm;

    // Print out times and SQL for RQL Queries
    protected final boolean useMetrics;
    protected final long metricsThreshold;

    protected final DatabaseType databaseType;
    protected final DSLContext create;

    public DaoUtils(DataSource dataSource, PlatformTransactionManager transactionManager, DatabaseType databaseType, DSLContext context, ExtendedJdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.databaseType = databaseType;
        this.useMetrics = Common.envProps.getBoolean("db.useMetrics", false);
        this.metricsThreshold = Common.envProps.getLong("db.metricsThreshold", 0L);
        this.ejt = jdbcTemplate;
        this.create = context;
    }

    public boolean isUseMetrics() {
        return this.useMetrics;
    }

    public long getMetricsThreshold() {
        return this.metricsThreshold;
    }

    protected void setInt(PreparedStatement stmt, int index, int value, int nullValue) throws SQLException {
        if (value == nullValue)
            stmt.setNull(index, Types.INTEGER);
        else
            stmt.setInt(index, value);
    }

    protected int getInt(ResultSet rs, int index, int nullValue) throws SQLException {
        int value = rs.getInt(index);
        if (rs.wasNull())
            return nullValue;
        return value;
    }

    public String dbEncodeSearchString(String value) {
        if (value == null)
            return null;
        return value.replaceAll("%", "\\\\%") + '%';
    }

    protected List<StringStringPair> createStringStringPairs(ResultSet rs) throws SQLException {
        List<StringStringPair> result = new ArrayList<>();
        while (rs.next())
            result.add(new StringStringPair(rs.getString(1), rs.getString(2)));
        return result;
    }

    protected List<IntStringPair> createIntStringPairs(ResultSet rs) throws SQLException {
        List<IntStringPair> result = new ArrayList<>();
        while (rs.next())
            result.add(new IntStringPair(rs.getInt(1), rs.getString(2)));
        return result;
    }

    //
    // Delimited lists
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

    protected int[] batchUpdate(String sql, final Object[][] args) {
        final List<ArgPreparedStatementSetter> statements = new ArrayList<>(args.length);
        for (Object[] arg : args) {
            statements.add(new ArgPreparedStatementSetter(arg));
        }

        try {
            return ejt.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public int getBatchSize() {
                    return args.length;
                }

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    (statements.get(i)).setValues(ps);
                }
            });
        }
        finally {
            for (int i = 0; i < args.length; i++)
                (statements.get(i)).cleanupParameters();
        }
    }

    //
    // Insert with int increment
    protected int doInsert(String sql, Object... params) {
        return ejt.doInsert(sql, params);
    }

    protected int doInsert(String sql, Object[] params, int[] types) {
        return ejt.doInsert(sql, params, types);
    }

    protected int doInsert(String sql, PreparedStatementSetter pss) {
        return ejt.doInsert(sql, pss);
    }

    protected int doInsert(String sql, String genField, Object... params) {
        return ejt.doInsert(sql, genField, params);
    }

    protected int doInsert(String sql, String genField, Object[] params, int[] types) {
        return ejt.doInsert(sql, genField, params, types);
    }

    protected int doInsert(String sql, String genField, PreparedStatementSetter pss) {
        return ejt.doInsert(sql, genField, pss);
    }

    //
    // Insert with long increment
    protected long doInsertLong(String sql, Object... params) {
        return ejt.doInsertLong(sql, params);
    }

    protected long doInsertLong(String sql, Object[] params, int[] types) {
        return ejt.doInsertLong(sql, params, types);
    }

    protected long doInsertLong(String sql, PreparedStatementSetter pss) {
        return ejt.doInsert(sql, pss);
    }

    protected long doInsertLong(String sql, String genField, Object... params) {
        return ejt.doInsertLong(sql, genField, params);
    }

    protected long doInsertLong(String sql, String genField, Object[] params, int[] types) {
        return ejt.doInsertLong(sql, genField, params, types);
    }

    protected long doInsertLong(String sql, String genField, PreparedStatementSetter pss) {
        return ejt.doInsertLong(sql, genField, pss);
    }

    protected Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    public <T> List<T> query(String sql, RowMapper<T> rowMapper) {
        return ejt.query(sql, rowMapper);
    }

    public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) {
        return ejt.query(sql, args, rowMapper);
    }

    public <T> T query(String sql, ResultSetExtractor<T> resultSetExtractor) {
        return ejt.query(sql, resultSetExtractor);
    }

    public <T> T query(String sql, Object[] args, ResultSetExtractor<T> resultSetExtractor) {
        return ejt.query(sql, args, resultSetExtractor);
    }

    public <T> List<T> queryForList(String sql, Class<T> elementType) {
        return ejt.queryForList(sql, elementType);
    }

    public <T> List<T> queryForList(String sql, Object[] args, Class<T> elementType) {
        return ejt.queryForList(sql, args, elementType);
    }

    public <T> T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper) {
        return ejt.queryForObject(sql, args, rowMapper);
    }

    public <T> @Nullable T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper, @Nullable T zeroResult) {
        return ejt.queryForObject(sql, args, rowMapper, zeroResult);
    }

    public <T> @Nullable T queryForObject(String sql, Object[] args, Class<T> requiredType, @Nullable T zeroResult) {
        return ejt.queryForObject(sql, args, requiredType, zeroResult);
    }

    public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper, int limit) {
        return ejt.query(sql, args, rowMapper, limit);
    }

    public <T> void query(String sql, Object[] args, final RowMapper<T> rowMapper, final Consumer<T> callback) {
        ejt.query(sql, args, new RowCallbackHandler() {
            private int rowNum = 0;
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                callback.accept(rowMapper.mapRow(rs, rowNum++));
            }
        });
    }

    //
    // Transaction management
    //
    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

}
