/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.tables.Audit;
import com.infiniteautomation.mango.db.tables.records.AuditRecord;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.util.LazyInitializer;
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
public class AuditEventDao extends AbstractBasicDao<AuditEventInstanceVO, AuditRecord, Audit> {
    private static final LazyInitializer<AuditEventDao> springInstance = new LazyInitializer<>();

    @Autowired
    private AuditEventDao(
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(Audit.AUDIT, mapper, publisher);
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static AuditEventDao getInstance() {
        return springInstance.get(() -> {
            return Common.getRuntimeContext().getBean(AuditEventDao.class);
        });
    }

    @Override
    protected Record voToObjectArray(AuditEventInstanceVO vo) {
        String jsonData = null;
        try{
            jsonData = writeValueAsString(vo.getContext());

        }catch(JsonException | IOException e){
            LOG.error(e.getMessage(), e);
        }

        Record record = table.newRecord();

        record.set(table.typeName, vo.getTypeName());
        record.set(table.alarmLevel, vo.getAlarmLevel().value());
        record.set(table.userId, vo.getUserId());
        record.set(table.changeType, vo.getChangeType());
        record.set(table.objectId, vo.getObjectId());
        record.set(table.ts, vo.getTimestamp());
        record.set(table.context, jsonData);
        record.set(table.message, writeTranslatableMessage(vo.getMessage()));

        return record;
    }

    @Override
    public AuditEventInstanceVO mapRecord(Record record) {
        int i=0;
        AuditEventInstanceVO vo = new AuditEventInstanceVO();
        vo.setId(record.get(table.id));
        vo.setTypeName(record.get(table.typeName));
        vo.setAlarmLevel(AlarmLevels.fromValue(record.get(table.alarmLevel)));
        vo.setUserId(record.get(table.userId));
        vo.setChangeType(record.get(table.changeType));
        vo.setObjectId(record.get(table.objectId));
        vo.setTimestamp(record.get(table.ts));
        try {
            vo.setContext(readValueFromString(record.get(table.context)));
        } catch (IOException | JsonException e) {
            LOG.error(e.getMessage(), e);
        }
        vo.setMessage(readTranslatableMessage(record.get(table.message)));
        return vo;
    }

    public JsonObject readValueFromString(String json) throws JsonException, IOException {
        JsonTypeReader reader = new JsonTypeReader(json);
        return (JsonObject) reader.read();
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
        return getJoinedSelectQuery()
                .where(table.typeName.eq(typeName), table.objectId.eq(id))
                .orderBy(table.ts.asc())
                .fetch(this::mapRecord);
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
     * @return
     */
    public int purgeEventsBefore(final long time) {
        return ejt.update("delete from audit where ts<?", new Object[] {time});
    }

    /**
     * Purge Audit Events Before a given time with a given alarmLevel
     * @param time
     * @return
     */
    public int purgeEventsBefore(final long time, final AlarmLevels alarmLevel) {
        return ejt.update("delete from audit where ts<? and alarmLevel=?", new Object[] {time, alarmLevel.value()});
    }
}
