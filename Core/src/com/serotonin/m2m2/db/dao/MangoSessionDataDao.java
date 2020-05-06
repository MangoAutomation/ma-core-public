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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.spring.db.MangoSessionDataTableDefinition;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.MangoSessionDataVO;

/**
 * Access to persist/retrieve Jetty session data
 * @author Terry Packer
 */
@Repository
public class MangoSessionDataDao extends BaseDao {

    protected final MangoSessionDataTableDefinition table;

    private static final LazyInitSupplier<MangoSessionDataDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(MangoSessionDataDao.class);
    });

    @Autowired
    private MangoSessionDataDao(MangoSessionDataTableDefinition table) {
        this.table = table;
    }

    public static MangoSessionDataDao getInstance() {
        return springInstance.get();
    }

    protected Object[] voToObjectArray(MangoSessionDataVO vo) {
        return new Object[] {
                vo.getSessionId(),
                vo.getContextPath(),
                vo.getVirtualHost(),
                vo.getLastNode(),
                vo.getAccessTime(),
                vo.getLastAccessTime(),
                vo.getCreateTime(),
                vo.getCookieTime(),
                vo.getLastSavedTime(),
                vo.getExpiryTime(),
                vo.getMaxInterval(),
                vo.getUserId()
        };
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
     * @param vo
     */
    public void insert(MangoSessionDataVO vo) throws DataAccessException {
        InsertValuesStepN<?> insert = this.create.insertInto(this.table.getTable()).columns(this.table.getInsertFields()).values(voToObjectArray(vo));
        insert.execute();
    }

    /**
     * Update session data
     * @param sessionId
     * @param contextPath
     * @param virtualHost
     * @param vo
     */
    public void update(String sessionId, String contextPath, String virtualHost, MangoSessionDataVO vo) {
        List<Object> list = new ArrayList<>();
        list.addAll(Arrays.asList(voToObjectArray(vo)));
        Map<Field<?>, Object> values = new LinkedHashMap<>();
        int i = 0;
        for(Field<?> f : this.table.getUpdateFields()) {
            values.put(f, list.get(i));
            i++;
        }
        UpdateConditionStep<?> update = this.create.update(this.table.getTableAsAlias()).set(values).where(
                this.table.getAlias("sessionId").eq(sessionId),
                this.table.getAlias("contextPath").eq(contextPath),
                this.table.getAlias("virtualHost").eq(virtualHost));

        String sql = update.getSQL();
        List<Object> args = update.getBindValues();
        ejt.update(sql, args.toArray(new Object[args.size()]));
    }

    /**
     * Is there a session with this primary key
     *
     * @param sessionId
     * @param contextPath
     * @param virtualHost
     * @return
     */
    public boolean sessionExists(String sessionId, String contextPath, String virtualHost) {
        Select<Record1<Object>> query = this.create.select(this.table.getAlias("expiryTime"))
                .from(this.table.getTableAsAlias())
                .where(this.table.getAlias("sessionId").eq(sessionId),
                        this.table.getAlias("contextPath").eq(contextPath),
                        this.table.getAlias("virtualHost").eq(virtualHost));

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

    /**
     * @param sessionId
     * @param contextPath
     * @param vhost
     * @return
     */
    public boolean delete(String sessionId, String contextPath, String virtualHost) {
        return this.create.deleteFrom(table.getTableAsAlias()).where(
                this.table.getAlias("sessionId").eq(sessionId),
                this.table.getAlias("contextPath").eq(contextPath),
                this.table.getAlias("virtualHost").eq(virtualHost)).execute() > 0;
    }

    /**
     * Get expired sessions before or at the upper bound
     *
     * @param canonicalContextPath
     * @param vhost
     * @param upperBound
     * @return
     */
    public Set<String> getExpiredSessionIds(String contextPath,
            String virtualHost, long upperBound) {
        Select<Record1<Object>> query = this.create.select(this.table.getAlias("sessionId"))
                .from(this.table.getTableAsAlias())
                .where(this.table.getAlias("contextPath").eq(contextPath),
                        this.table.getAlias("virtualHost").eq(virtualHost),
                        this.table.getAlias("expiryTime").greaterThan(0),
                        this.table.getAlias("expiryTime").lessOrEqual(upperBound));
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

    /**
     *
     * @param sessionId
     * @param contextPath
     * @param virtualHost
     * @return
     */
    public MangoSessionDataVO get(String sessionId, String contextPath, String virtualHost) {
        Select<Record> query = this.create.select(this.table.getSelectFields())
                .from(this.table.getTableAsAlias())
                .where(this.table.getAlias("sessionId").eq(sessionId),
                        this.table.getAlias("contextPath").eq(contextPath),
                        this.table.getAlias("virtualHost").eq(virtualHost))
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
     * @param id
     */
    public boolean deleteSessionsForUser(int userId) {
        return this.create.deleteFrom(table.getTableAsAlias()).where(
                this.table.getAlias("userId").eq(userId)).execute() > 0;
    }

}
