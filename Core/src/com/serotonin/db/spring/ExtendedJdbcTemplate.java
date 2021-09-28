package com.serotonin.db.spring;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Extends the Spring Framework JdbcTemplate object, adding several convenience methods.
 *
 * @author mlohbihler
 */
public class ExtendedJdbcTemplate extends JdbcTemplate {
    //    final Log logger = super.logger;

    public ExtendedJdbcTemplate(DataSource dataSource) {
        super(dataSource);
    }

    public <T> @Nullable T optionalUniqueResult(List<T> results, @Nullable T zeroResult) {
        T result = DataAccessUtils.uniqueResult(results);
        if (result == null)
            return zeroResult;
        return result;
    }

    public <T> @Nullable T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper, @Nullable T zeroResult) {
        List<T> results = query(sql, new RowMapperResultSetExtractor<T>(rowMapper, 1), args);
        return optionalUniqueResult(results, zeroResult);
    }

    public <T> @Nullable T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper, @Nullable T zeroResult) {
        List<T> results = query(sql, args, argTypes, new RowMapperResultSetExtractor<T>(rowMapper, 1));
        return optionalUniqueResult(results, zeroResult);
    }

    public <T> @Nullable T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType, @Nullable T zeroResult) {
        return queryForObject(sql, args, argTypes, getSingleColumnRowMapper(requiredType), zeroResult);
    }

    public long queryForLong(String sql, Object[] args, int[] argTypes, long zeroResult) {
        Number number = queryForObject(sql, args, argTypes, Long.class, null);
        return (number != null ? number.longValue() : zeroResult);
    }

    public int queryForInt(String sql, Object[] args, int[] argTypes, int zeroResult) {
        Number number = queryForObject(sql, args, argTypes, Integer.class, null);
        return (number != null ? number.intValue() : zeroResult);
    }

    public <T> @Nullable T queryForObject(String sql, Object[] args, Class<T> requiredType, @Nullable T zeroResult) {
        return queryForObject(sql, args, getSingleColumnRowMapper(requiredType), zeroResult);
    }

    public long queryForLong(String sql, Object[] args, long zeroResult) throws DataAccessException {
        Number number = queryForObject(sql, args, Long.class, null);
        return (number != null ? number.longValue() : zeroResult);
    }

    public int queryForInt(String sql, Object[] args, int zeroResult) throws DataAccessException {
        Number number = queryForObject(sql, args, Integer.class, null);
        return (number != null ? number.intValue() : zeroResult);
    }

    @Override
    public int update(String sql, Object... args) throws DataAccessException {
        return update(sql, new ArgPreparedStatementSetter(args));
    }

    @Override
    public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return update(sql, new ArgTypePreparedStatementSetter(args, argTypes));
    }

    public int update(String sql, Object[] args, KeyHolder keyHolder) throws DataAccessException {
        return update(sql, new ArgPreparedStatementSetter(args), keyHolder);
    }

    public int update(String sql, Object[] args, int[] argTypes, KeyHolder keyHolder) throws DataAccessException {
        return update(sql, new ArgTypePreparedStatementSetter(args, argTypes), keyHolder);
    }

    public int update(String sql, PreparedStatementSetter pss, KeyHolder keyHolder) throws DataAccessException {
        return update(new KeyGeneratingPreparedStatementCreator(sql), pss, keyHolder);
    }

    public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper, int limit) throws DataAccessException {
        return query(sql, args, new RowMapperResultSetLimitExtractor<T>(rowMapper, limit));
    }

    /**
     * Combines two of the methods from the super class to create one that seems to oddly be missing.
     */
    protected int update(final PreparedStatementCreator psc, final PreparedStatementSetter pss,
            final KeyHolder generatedKeyHolder) throws DataAccessException {
        if (logger.isDebugEnabled()) {
            // Unfortunately, the getSql method is private. We can probably manage without.
            logger.debug("Executing SQL update and returning generated keys");
        }

        Integer result = execute(psc, new PreparedStatementCallback<Integer>() {
            @Override
            @SuppressWarnings("synthetic-access")
            public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException {
                try {
                    if (pss != null) {
                        pss.setValues(ps);
                    }
                    int rows = ps.executeUpdate();
                    List<Map<String, Object>> generatedKeys = generatedKeyHolder.getKeyList();
                    generatedKeys.clear();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (keys != null) {
                        try {
                            RowMapper<Map<String, Object>> rowMapper = getColumnMapRowMapperImpl();
                            RowMapperResultSetExtractor<Map<String, Object>> rse = new RowMapperResultSetExtractor<Map<String, Object>>(
                                    rowMapper, 1);
                            generatedKeys.addAll(rse.extractData(keys));
                        }
                        finally {
                            JdbcUtils.closeResultSet(keys);
                        }
                    }
                    if (logger.isDebugEnabled())
                        logger.debug("SQL update affected " + rows + " rows and returned " + generatedKeys.size()
                        + " keys");
                    return Integer.valueOf(rows);
                }
                finally {
                    if (pss instanceof ParameterDisposer) {
                        ((ParameterDisposer) pss).cleanupParameters();
                    }
                }
            }
        });
        return result.intValue();
    }

    RowMapper<Map<String, Object>> getColumnMapRowMapperImpl() {
        return super.getColumnMapRowMapper();
    }

    public int doInsert(String sql, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, params, keyHolder);
        return getIntIdKey(keyHolder);
    }

    public int doInsert(String sql, Object[] params, int[] types) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, params, types, keyHolder);
        return getIntIdKey(keyHolder);
    }

    public int doInsert(String sql, PreparedStatementSetter pss) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, pss, keyHolder);
        return getIntIdKey(keyHolder);
    }

    public int doInsert(String sql, String genField, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, params, keyHolder);
        return getIntIdKey(keyHolder, genField);
    }

    public int doInsert(String sql, String genField, Object[] params, int[] types) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, params, types, keyHolder);
        return getIntIdKey(keyHolder, genField);
    }

    public int doInsert(String sql, String genField, PreparedStatementSetter pss) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, pss, keyHolder);
        return getIntIdKey(keyHolder, genField);
    }

    //
    // Insert with long increment
    public long doInsertLong(String sql, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, params, keyHolder);
        return getLongIdKey(keyHolder);
    }

    public long doInsertLong(String sql, Object[] params, int[] types) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, params, types, keyHolder);
        return getLongIdKey(keyHolder);
    }

    public long doInsertLong(String sql, PreparedStatementSetter pss) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, pss, keyHolder);
        return getLongIdKey(keyHolder);
    }

    public long doInsertLong(String sql, String genField, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, params, keyHolder);
        return getLongIdKey(keyHolder, genField);
    }

    public long doInsertLong(String sql, String genField, Object[] params, int[] types) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, params, types, keyHolder);
        return getLongIdKey(keyHolder, genField);
    }

    public long doInsertLong(String sql, String genField, PreparedStatementSetter pss) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        update(sql, pss, keyHolder);
        return getLongIdKey(keyHolder, genField);
    }

    private int getIntIdKey(GeneratedKeyHolder keyHolder) {
        Object key = getIdKey(keyHolder);
        if (key == null)
            return -1;
        return ((Number) key).intValue();
    }

    private long getLongIdKey(GeneratedKeyHolder keyHolder) {
        Object key = getIdKey(keyHolder);
        if (key == null)
            return -1;
        return ((Number) key).longValue();
    }

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

    private int getIntIdKey(GeneratedKeyHolder keyHolder, String name) {
        Object key = getIdKey(keyHolder, name);
        if (key == null)
            return -1;
        return ((Number) key).intValue();
    }

    private long getLongIdKey(GeneratedKeyHolder keyHolder, String name) {
        Object key = getIdKey(keyHolder, name);
        if (key == null)
            return -1;
        return ((Number) key).longValue();
    }

    private Object getIdKey(GeneratedKeyHolder keyHolder, String name) {
        List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (keyList.size() == 0)
            return null;
        if (keyList.size() > 1)
            throw new InvalidDataAccessApiUsageException("Multiple key records returned from insert");
        return keyList.get(0).get(name);
    }

    public <T> void query(String sql, Object[] args, RowMapper<T> rowMapper, Consumer<T> callback) {
        query(sql, new ConsumerRowCallbackHandler<>(rowMapper, callback), args);
    }
}
