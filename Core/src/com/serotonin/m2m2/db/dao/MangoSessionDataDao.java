/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jooq.Field;
import org.jooq.InsertValuesStepN;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.UpdateConditionStep;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.tables.MangoSessionData;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.MangoSessionDataVO;

/**
 * Access to persist/retrieve Jetty session data
 * @author Terry Packer
 */
@Repository
public class MangoSessionDataDao extends BaseDao {

    protected final MangoSessionData table = MangoSessionData.MANGO_SESSION_DATA;

    private static final LazyInitSupplier<MangoSessionDataDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(MangoSessionDataDao.class);
    });

    public static MangoSessionDataDao getInstance() {
        return springInstance.get();
    }

    protected Record voToObjectArray(MangoSessionDataVO vo) {
        Record record = table.newRecord();
        record.set(table.sessionId, vo.getSessionId());
        record.set(table.contextPath, vo.getContextPath());
        record.set(table.virtualHost, vo.getVirtualHost());
        record.set(table.lastNode, vo.getLastNode());
        record.set(table.accessTime, vo.getAccessTime());
        record.set(table.lastAccessTime, vo.getLastAccessTime());
        record.set(table.createTime, vo.getCreateTime());
        record.set(table.cookieTime, vo.getCookieTime());
        record.set(table.lastSavedTime, vo.getLastSavedTime());
        record.set(table.expiryTime, vo.getExpiryTime());
        record.set(table.maxInterval, vo.getMaxInterval());
        record.set(table.userId, vo.getUserId());
        return record;
    }

    public RowMapper<MangoSessionDataVO> getRowMapper() {
        return mapper;
    }

    private final MangoSessionDataRowMapper mapper = new MangoSessionDataRowMapper();

    private class MangoSessionDataRowMapper implements RowMapper<MangoSessionDataVO> {

        @Override
        public MangoSessionDataVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i=0;
            MangoSessionDataVO vo = new MangoSessionDataVO();
            vo.setSessionId(rs.getString(++i));
            vo.setContextPath(rs.getString(++i));
            vo.setVirtualHost(rs.getString(++i));
            vo.setLastNode(rs.getString(++i));
            vo.setAccessTime(rs.getLong(++i));
            vo.setLastAccessTime(rs.getLong(++i));
            vo.setCreateTime(rs.getLong(++i));
            vo.setCookieTime(rs.getLong(++i));
            vo.setLastSavedTime(rs.getLong(++i));
            vo.setExpiryTime(rs.getLong(++i));
            vo.setMaxInterval(rs.getLong(++i));
            vo.setUserId(rs.getInt(++i));
            return vo;
        }

    }

    /**
     * Insert session data
     */
    public void insert(MangoSessionDataVO vo) throws DataAccessException {
        InsertValuesStepN<?> insert = this.create.insertInto(table).columns(table.fields()).values(voToObjectArray(vo));
        insert.execute();
    }

    /**
     * Update session data
     */
    public void update(String sessionId, String contextPath, String virtualHost, MangoSessionDataVO vo) {
        List<Object> list = new ArrayList<>();
        // TODO Mango 4.0
        list.addAll(Arrays.asList(voToObjectArray(vo)));
        Map<Field<?>, Object> values = new LinkedHashMap<>();
        int i = 0;
        for(Field<?> f : table.fields()) {
            values.put(f, list.get(i));
            i++;
        }
        UpdateConditionStep<?> update = this.create.update(table).set(values).where(
                table.sessionId.eq(sessionId),
                table.contextPath.eq(contextPath),
                table.virtualHost.eq(virtualHost));

        String sql = update.getSQL();
        List<Object> args = update.getBindValues();
        ejt.update(sql, args.toArray(new Object[args.size()]));
    }

    /**
     * Is there a session with this primary key
     */
    public boolean sessionExists(String sessionId, String contextPath, String virtualHost) {
        Select<Record1<Long>> query = this.create.select(table.expiryTime)
                .from(table)
                .where(table.sessionId.eq(sessionId),
                        table.contextPath.eq(contextPath),
                        table.virtualHost.eq(virtualHost));

        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        Long expiryTime = queryForObject(sql, args.toArray(), Long.class, null);

        if(expiryTime == null) {
            return false;
        }else if(expiryTime <=0) {
            return true; //Never expires
        }else {
            return expiryTime > Common.timer.currentTimeMillis();
        }

    }

    public boolean delete(String sessionId, String contextPath, String virtualHost) {
        return this.create.deleteFrom(table).where(
                table.sessionId.eq(sessionId),
                table.contextPath.eq(contextPath),
                table.virtualHost.eq(virtualHost)).execute() > 0;
    }

    /**
     * Get expired sessions before or at the upper bound
     */
    public Set<String> getExpiredSessionIds(String contextPath,
            String virtualHost, long upperBound) {
        Select<Record1<String>> query = this.create.select(table.sessionId)
                .from(table)
                .where(table.contextPath.eq(contextPath),
                        table.virtualHost.eq(virtualHost),
                        table.expiryTime.greaterThan(0L),
                        table.expiryTime.lessOrEqual(upperBound));
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        return query(sql, args.toArray(), expiredIdsExtractor);
    }

    private final ExpireSessionIdsExtractor expiredIdsExtractor = new ExpireSessionIdsExtractor();
    private class ExpireSessionIdsExtractor implements ResultSetExtractor<Set<String>> {
        @Override
        public Set<String> extractData(ResultSet rs) throws SQLException, DataAccessException {
            Set<String> expired = new HashSet<>();
            while (rs.next()) {
                expired.add(rs.getString(1));
            }
            return expired;
        }
    }

    public MangoSessionDataVO get(String sessionId, String contextPath, String virtualHost) {
        Select<Record> query = this.create.select(Arrays.asList(table.fields()))
                .from(table)
                .where(table.sessionId.eq(sessionId),
                        table.contextPath.eq(contextPath),
                        table.virtualHost.eq(virtualHost))
                .limit(1);
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        return ejt.query(sql, args.toArray(new Object[args.size()]), extractor);
    }

    private final MangoSessionDataVOExtractor extractor = new MangoSessionDataVOExtractor();
    private class MangoSessionDataVOExtractor implements ResultSetExtractor<MangoSessionDataVO> {
        @Override
        public MangoSessionDataVO extractData(ResultSet rs) throws SQLException, DataAccessException {
            RowMapper<MangoSessionDataVO> rowMapper = getRowMapper();
            List<MangoSessionDataVO> results = new ArrayList<>();
            int rowNum = 0;
            while (rs.next()) {
                MangoSessionDataVO row = rowMapper.mapRow(rs, rowNum);
                results.add(row);
                rowNum++;
                return DataAccessUtils.uniqueResult(results);
            }
            return null;
        }
    }

    /**
     * Delete all session entries for this user
     */
    public boolean deleteSessionsForUser(int userId) {
        return this.create.deleteFrom(table).where(
                table.userId.eq(userId)).execute() > 0;
    }

}
