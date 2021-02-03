/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.Set;

import org.jooq.Record;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.tables.MangoSessionData;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.MangoSessionDataVO;

/**
 * Access to persist/retrieve Jetty session data
 *
 * @author Terry Packer
 */
@Repository
public class MangoSessionDataDao extends BaseDao {

    protected final MangoSessionData table = MangoSessionData.MANGO_SESSION_DATA;

    private static final LazyInitSupplier<MangoSessionDataDao> springInstance = new LazyInitSupplier<>(() ->
            Common.getRuntimeContext().getBean(MangoSessionDataDao.class));

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

    public MangoSessionDataVO mapRecord(Record record) {
        MangoSessionDataVO vo = new MangoSessionDataVO();
        vo.setSessionId(record.get(table.sessionId));
        vo.setContextPath(record.get(table.contextPath));
        vo.setVirtualHost(record.get(table.virtualHost));
        vo.setLastNode(record.get(table.lastNode));
        vo.setAccessTime(record.get(table.accessTime));
        vo.setLastAccessTime(record.get(table.lastAccessTime));
        vo.setCreateTime(record.get(table.createTime));
        vo.setCookieTime(record.get(table.cookieTime));
        vo.setLastSavedTime(record.get(table.lastSavedTime));
        vo.setExpiryTime(record.get(table.expiryTime));
        vo.setMaxInterval(record.get(table.maxInterval));
        vo.setUserId(record.get(table.userId));
        return vo;
    }

    /**
     * Insert session data
     */
    public void insert(MangoSessionDataVO vo) throws DataAccessException {
        this.create.insertInto(table).set(voToObjectArray(vo)).execute();
    }

    /**
     * Update session data
     */
    public void update(String sessionId, String contextPath, String virtualHost, MangoSessionDataVO vo) {
        this.create.update(table)
                .set(voToObjectArray(vo))
                .where(table.sessionId.eq(sessionId),
                        table.contextPath.eq(contextPath),
                        table.virtualHost.eq(virtualHost))
                .execute();
    }

    /**
     * Is there a session with this primary key
     */
    public boolean sessionExists(String sessionId, String contextPath, String virtualHost) {
        Long expiryTime = this.create.select(table.expiryTime)
                .from(table)
                .where(table.sessionId.eq(sessionId),
                        table.contextPath.eq(contextPath),
                        table.virtualHost.eq(virtualHost))
                .fetchSingle().value1();

        if (expiryTime == null) {
            return false;
        } else if (expiryTime <= 0) {
            return true; //Never expires
        } else {
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

        return this.create.select(table.sessionId)
                .from(table)
                .where(table.contextPath.eq(contextPath),
                        table.virtualHost.eq(virtualHost),
                        table.expiryTime.greaterThan(0L),
                        table.expiryTime.lessOrEqual(upperBound))
                .fetchSet(table.sessionId);
    }

    public MangoSessionDataVO get(String sessionId, String contextPath, String virtualHost) {
        return this.create.select(table.fields())
                .from(table)
                .where(table.sessionId.eq(sessionId),
                        table.contextPath.eq(contextPath),
                        table.virtualHost.eq(virtualHost))
                .fetchOne(this::mapRecord);
    }

    /**
     * Delete all session entries for this user
     */
    public boolean deleteSessionsForUser(int userId) {
        return this.create.deleteFrom(table).where(
                table.userId.eq(userId)).execute() > 0;
    }

}
