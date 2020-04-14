/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.AuditEventTableDefinition;
import com.infiniteautomation.mango.util.LazyInitializer;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class AuditEventDao extends AbstractBasicDao<AuditEventInstanceVO, AuditEventTableDefinition> {
    private static final LazyInitializer<AuditEventDao> springInstance = new LazyInitializer<>();

    /**
     * @param tablePrefix
     * @param extraProperties
     * @param extraSQL
     */
    @Autowired
    private AuditEventDao(
            AuditEventTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(table, mapper, publisher);
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static AuditEventDao getInstance() {
        return springInstance.get(() -> {
            Object o = Common.getRuntimeContext().getBean(AuditEventDao.class);
            if(o == null)
                throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
            return (AuditEventDao)o;
        });
    }

    @Override
    protected Object[] voToObjectArray(AuditEventInstanceVO vo) {
        String jsonData = null;
        try{
            jsonData = writeValueAsString(vo.getContext());

        }catch(JsonException | IOException e){
            LOG.error(e.getMessage(), e);
        }

        return new Object[]{
                vo.getTypeName(),
                vo.getAlarmLevel().value(),
                vo.getUserId(),
                vo.getChangeType(),
                vo.getObjectId(),
                vo.getTimestamp(),
                jsonData,
                writeTranslatableMessage(vo.getMessage())
        };
    }

    @Override
    public RowMapper<AuditEventInstanceVO> getRowMapper() {
        return new AuditEventInstanceRowMapper();
    }

    class AuditEventInstanceRowMapper implements RowMapper<AuditEventInstanceVO>{

        @Override
        public AuditEventInstanceVO mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            int i=0;
            AuditEventInstanceVO vo = new AuditEventInstanceVO();
            vo.setId(rs.getInt(++i));
            vo.setTypeName(rs.getString(++i));
            vo.setAlarmLevel(AlarmLevels.fromValue(rs.getInt(++i)));
            vo.setUserId(rs.getInt(++i));
            vo.setChangeType(rs.getInt(++i));
            vo.setObjectId(rs.getInt(++i));
            vo.setTimestamp(rs.getLong(++i));
            try {
                vo.setContext(readValueFromString(rs.getString(++i)));
            } catch (IOException | JsonException e) {
                LOG.error(e.getMessage(), e);
            }
            vo.setMessage(readTranslatableMessage(rs, ++i));
            return vo;
        }

    }

    public JsonObject readValueFromString(String json) throws JsonException, IOException {
        JsonTypeReader reader = new JsonTypeReader(json);
        return (JsonObject)reader.read();
    }

    public String writeValueAsString(JsonObject value) throws JsonException, IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.writeObject(value);
        return stringWriter.toString();

    }

    /**
     * Get the audit trail in time ascending order for this object
     *
     * @param id
     * @return
     */
    public List<AuditEventInstanceVO> getAllForObject(String typeName, int id) {
        String selectAll = this.getJoinedSelectQuery().getSQL();
        return query(selectAll + " WHERE typeName=? AND objectId=? ORDER BY ts ASC", new Object[]{typeName, id}, getRowMapper());
    }

    /**
     * Purge all Audit Events
     * @return
     */
    public int purgeAllEvents(){
        return ejt.update("delete from audit");

    }

    /**
     * Purge Audit Events Before a given time
     * @param time
     * @param typeName
     * @return
     */
    public int purgeEventsBefore(final long time) {
        return ejt.update("delete from audit where ts<?", new Object[] {time});
    }

    /**
     * Purge Audit Events Before a given time with a given alarmLevel
     * @param time
     * @param typeName
     * @return
     */
    public int purgeEventsBefore(final long time, final AlarmLevels alarmLevel) {
        return ejt.update("delete from audit where ts<? and alarmLevel=?", new Object[] {time, alarmLevel.value()});
    }
}
