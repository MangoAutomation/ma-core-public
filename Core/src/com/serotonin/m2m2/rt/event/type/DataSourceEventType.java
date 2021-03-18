/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;
import java.util.Map;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class DataSourceEventType extends EventType {
    private int dataSourceId;
    private int dataSourceEventTypeId;
    private AlarmLevels alarmLevel;
    private DuplicateHandling duplicateHandling;

    public DataSourceEventType() {
        // Required for reflection.
    }

    public DataSourceEventType(int dataSourceId, int dataSourceEventTypeId) {
        this(dataSourceId, dataSourceEventTypeId, AlarmLevels.URGENT, DuplicateHandling.IGNORE);
    }

    public DataSourceEventType(DataSourceVO ds, int dataSourceEventTypeId, AlarmLevels alarmLevel, DuplicateHandling duplicateHandling) {
        this.dataSourceId = ds.getId();
        this.dataSourceEventTypeId = dataSourceEventTypeId;
        this.alarmLevel = alarmLevel;
        this.duplicateHandling = duplicateHandling;
        supplyReference1(() -> {
            return ds;
        });
        supplyReference2(() -> {
            return this;
        });
    }

    public DataSourceEventType(int dataSourceId, int dataSourceEventTypeId, AlarmLevels alarmLevel, DuplicateHandling duplicateHandling) {
        this.dataSourceId = dataSourceId;
        this.dataSourceEventTypeId = dataSourceEventTypeId;
        this.alarmLevel = alarmLevel;
        this.duplicateHandling = duplicateHandling;
        supplyReference1(() -> {
            return Common.getBean(DataSourceDao.class).get(dataSourceId);
        });
        supplyReference2(() -> {
            return this;
        });
    }

    @Override
    public String getEventType() {
        return EventType.EventTypeNames.DATA_SOURCE;
    }

    @Override
    public String getEventSubtype() {
        return null;
    }

    public int getDataSourceEventTypeId() {
        return dataSourceEventTypeId;
    }

    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    @Override
    public int getDataSourceId() {
        return dataSourceId;
    }

    @Override
    public String toString() {
        return "DataSoureEventType(dataSourceId=" + dataSourceId + ", eventTypeId=" + dataSourceEventTypeId + ")";
    }

    @Override
    public DuplicateHandling getDuplicateHandling() {
        return duplicateHandling;
    }

    @Override
    public int getReferenceId1() {
        return dataSourceId;
    }

    @Override
    public int getReferenceId2() {
        return dataSourceEventTypeId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + dataSourceEventTypeId;
        result = prime * result + dataSourceId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataSourceEventType other = (DataSourceEventType) obj;
        if (dataSourceEventTypeId != other.dataSourceEventTypeId)
            return false;
        if (dataSourceId != other.dataSourceId)
            return false;
        return true;
    }

    //
    //
    // Serialization
    //
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        DataSourceVO ds = getDataSource(jsonObject, "XID");
        dataSourceId = ds.getId();
        dataSourceEventTypeId = getInt(jsonObject, "dataSourceEventType", ds.getEventCodes());
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        DataSourceVO ds = DataSourceDao.getInstance().get(dataSourceId);
        writer.writeEntry("XID", ds.getXid());
        writer.writeEntry("dataSourceEventType", ds.getEventCodes().getCode(dataSourceEventTypeId));
    }

    @Override
    public boolean hasPermission(PermissionHolder user, PermissionService service) {
        return service.hasDataSourceReadPermission(user, dataSourceId);
    }

    @Override
    public MangoPermission getEventPermission(Map<String, Object> context, PermissionService service) {
        DataSourceVO vo = (DataSourceVO)context.get(DataSourceRT.DATA_SOURCE_EVENT_CONTEXT_KEY);
        if(vo != null) {
            return vo.getReadPermission();
        }else {
            return MangoPermission.superadminOnly();
        }
    }
}
