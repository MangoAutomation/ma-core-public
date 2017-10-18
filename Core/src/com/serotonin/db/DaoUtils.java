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
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.db.spring.ArgPreparedStatementSetter;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy.DatabaseType;

public class DaoUtils {
    protected final DataSource dataSource;
    protected final ExtendedJdbcTemplate ejt;
    protected DataSourceTransactionManager tm;

    protected final DatabaseType databaseType;
    protected final DSLContext create;
    protected final SQLDialect sqlDialect;
    
    public DaoUtils(DataSource dataSource) {
        this.dataSource = dataSource;
        this.databaseType = Common.databaseProxy.getType();
        
        ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(dataSource);

        switch(this.databaseType) {
            case DERBY:
                sqlDialect = SQLDialect.DERBY;
                break;
            case H2:
                sqlDialect = SQLDialect.H2;
                break;
            case MYSQL:
                sqlDialect = SQLDialect.MYSQL;
                break;
            case POSTGRES:
                sqlDialect = SQLDialect.POSTGRES;
                break;
            case MSSQL:
            default:
                sqlDialect = SQLDialect.DEFAULT;
                break;
        }
        
        Configuration configuration = new DefaultConfiguration();
        configuration.set(new SpringConnectionProvider(dataSource));
        configuration.set(new SpringTransactionProvider(getTransactionManager()));
        configuration.set(sqlDialect);
        
        this.create = DSL.using(configuration);
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
        List<StringStringPair> result = new ArrayList<StringStringPair>();
        while (rs.next())
            result.add(new StringStringPair(rs.getString(1), rs.getString(2)));
        return result;
    }

    protected List<IntStringPair> createIntStringPairs(ResultSet rs) throws SQLException {
        List<IntStringPair> result = new ArrayList<IntStringPair>();
        while (rs.next())
            result.add(new IntStringPair(rs.getInt(1), rs.getString(2)));
        return result;
    }

    //
    // Delimited lists
    protected String createDelimitedList(Collection<?> values, String delimeter, String quote) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> iterator = values.iterator();
        boolean first = true;
        while (iterator.hasNext()) {
            if (first)
                first = false;
            else
                sb.append(delimeter);

            if (quote != null)
                sb.append(quote);

            sb.append(iterator.next());

            if (quote != null)
                sb.append(quote);
        }
        return sb.toString();
    }

    protected String createDelimitedList(List<?> values, int from, int to, String delimeter, String quote) {
        if (from < 0)
            from = 0;
        if (to > values.size())
            to = values.size();

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = from; i < to; i++) {
            if (first)
                first = false;
            else
                sb.append(delimeter);

            if (quote != null)
                sb.append(quote);

            sb.append(values.get(i));

            if (quote != null)
                sb.append(quote);
        }
        return sb.toString();
    }

    protected int[] batchUpdate(String sql, final Object[][] args) {
        final List<ArgPreparedStatementSetter> apsss = new ArrayList<ArgPreparedStatementSetter>(args.length);
        for (int i = 0; i < args.length; i++)
            apsss.add(new ArgPreparedStatementSetter(args[i]));

        try {
            return ejt.batchUpdate(sql, new BatchPreparedStatementSetter() {
                public int getBatchSize() {
                    return args.length;
                }

                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    (apsss.get(i)).setValues(ps);
                }
            });
        }
        finally {
            for (int i = 0; i < args.length; i++)
                (apsss.get(i)).cleanupParameters();
        }
    }

    //
    // Insert with int increment
    @Deprecated
    protected int doInsert(String sql, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, params, keyHolder);
        return getIntIdKey(keyHolder);
    }

    @Deprecated
    protected int doInsert(String sql, Object[] params, int[] types) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, params, types, keyHolder);
        return getIntIdKey(keyHolder);
    }

    @Deprecated
    protected int doInsert(String sql, PreparedStatementSetter pss) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, pss, keyHolder);
        return getIntIdKey(keyHolder);
    }

    @Deprecated
    protected int doInsert(String sql, String genField, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, params, keyHolder);
        return getIntIdKey(keyHolder, genField);
    }

    @Deprecated
    protected int doInsert(String sql, String genField, Object[] params, int[] types) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, params, types, keyHolder);
        return getIntIdKey(keyHolder, genField);
    }

    @Deprecated
    protected int doInsert(String sql, String genField, PreparedStatementSetter pss) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, pss, keyHolder);
        return getIntIdKey(keyHolder, genField);
    }

    //
    // Insert with long increment
    @Deprecated
    protected long doInsertLong(String sql, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, params, keyHolder);
        return getLongIdKey(keyHolder);
    }

    @Deprecated
    protected long doInsertLong(String sql, Object[] params, int[] types) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, params, types, keyHolder);
        return getLongIdKey(keyHolder);
    }

    @Deprecated
    protected long doInsertLong(String sql, PreparedStatementSetter pss) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, pss, keyHolder);
        return getLongIdKey(keyHolder);
    }

    @Deprecated
    protected long doInsertLong(String sql, String genField, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, params, keyHolder);
        return getLongIdKey(keyHolder, genField);
    }

    @Deprecated
    protected long doInsertLong(String sql, String genField, Object[] params, int[] types) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, params, types, keyHolder);
        return getLongIdKey(keyHolder, genField);
    }

    @Deprecated
    protected long doInsertLong(String sql, String genField, PreparedStatementSetter pss) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        ejt.update(sql, pss, keyHolder);
        return getLongIdKey(keyHolder, genField);
    }

    @Deprecated
    private int getIntIdKey(GeneratedKeyHolder keyHolder) {
        Object key = getIdKey(keyHolder);
        if (key == null)
            return -1;
        return ((Number) key).intValue();
    }

    @Deprecated
    private long getLongIdKey(GeneratedKeyHolder keyHolder) {
        Object key = getIdKey(keyHolder);
        if (key == null)
            return -1;
        return ((Number) key).longValue();
    }

    @Deprecated
    private Object getIdKey(GeneratedKeyHolder keyHolder) {
        List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (keyList.size() == 0)
            return null;
        if (keyList.size() > 1)
            throw new InvalidDataAccessApiUsageException("Multiple key records returned from insert");
        Map<String, Object> keys = keyList.get(0);
        if (keys.size() == 0)
            return null;
        if (keys.size() == 1)
            return keys.values().iterator().next();

        // Look for "id"
        Object key = keys.get("id");
        if (key == null)
            key = keys.get("ID");
        if (key == null)
            throw new InvalidDataAccessApiUsageException("No obvious id field found in keys: " + keys);
        return key;
    }

    @Deprecated
    private int getIntIdKey(GeneratedKeyHolder keyHolder, String name) {
        Object key = getIdKey(keyHolder, name);
        if (key == null)
            return -1;
        return ((Number) key).intValue();
    }

    @Deprecated
    private long getLongIdKey(GeneratedKeyHolder keyHolder, String name) {
        Object key = getIdKey(keyHolder, name);
        if (key == null)
            return -1;
        return ((Number) key).longValue();
    }

    @Deprecated
    private Object getIdKey(GeneratedKeyHolder keyHolder, String name) {
        List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (keyList.size() == 0)
            return null;
        if (keyList.size() > 1)
            throw new InvalidDataAccessApiUsageException("Multiple key records returned from insert");
        return keyList.get(0).get(name);
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

    public <T> T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper, T zeroResult) {
        return ejt.queryForObject(sql, args, rowMapper, zeroResult);
    }

    public <T> T queryForObject(String sql, Object[] args, Class<T> requiredType, T zeroResult) {
        return ejt.queryForObject(sql, args, requiredType, zeroResult);
    }

    public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper, int limit) {
        return ejt.query(sql, args, rowMapper, limit);
    }

    public <T> void query(String sql, Object[] args, final RowMapper<T> rowMapper, final MappedRowCallback<T> callback) {
        ejt.query(sql, args, new RowCallbackHandler() {
            private int rowNum = 0;

            public void processRow(ResultSet rs) throws SQLException {
                callback.row(rowMapper.mapRow(rs, rowNum), rowNum);
                rowNum++;
            }
        });
    }

    public static byte[] uuidToBytes(UUID uuid) {
        byte[] b = new byte[16];
        long l = uuid.getMostSignificantBits();
        for (int i = 0; i < 8; i++) {
            b[7 - i] = (byte) (0xFF & l);
            l >>>= 8;
        }
        l = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            b[15 - i] = (byte) (0xFF & l);
            l >>>= 8;
        }
        return b;
    }

    public static UUID bytesToUuid(byte[] b) {
        long msb = 0, lsb = 0;
        msb |= b[0];
        for (int i = 1; i < 8; i++) {
            msb <<= 8;
            msb |= b[i];
        }
        lsb |= b[8];
        for (int i = 1; i < 8; i++) {
            lsb <<= 8;
            lsb |= b[i + 8];
        }
        return new UUID(msb, lsb);
    }

    //
    // Transaction management
    //
    protected DataSourceTransactionManager getTransactionManager() {
        // Lazy instantiation
        if (tm == null)
            tm = new DataSourceTransactionManager(dataSource);
        return tm;
    }

    protected TransactionTemplate getTransactionTemplate() {
        return new TransactionTemplate(getTransactionManager());
    }
}
