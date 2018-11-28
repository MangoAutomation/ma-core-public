/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.DuplicateHandling;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 *
 * @author Terry Packer
 */
public class DataSourceEventTypeModel extends EventTypeModel{

    private int dataSourceId;
    private int dataSourceEventTypeId;
    private AlarmLevels alarmLevel;
    private DuplicateHandling duplicateHandling;

    public DataSourceEventTypeModel(){}

    public DataSourceEventTypeModel(DataSourceEventType type){
        this.dataSourceId = type.getDataSourceId();
        this.dataSourceEventTypeId = type.getDataSourceEventTypeId();
        this.alarmLevel = type.getAlarmLevel();
        this.duplicateHandling = type.getDuplicateHandling();
    }

    @Override
    public String getTypeName(){
        return EventType.EventTypeNames.DATA_SOURCE;
    }

    @Override
    public boolean isRateLimited() {
        return true;
    }

    @Override
    public DuplicateHandling getDuplicateHandling() {
        return this.duplicateHandling;
    }

    public void setDuplicateHandling(DuplicateHandling duplicateHandling){
        this.duplicateHandling = duplicateHandling;
    }

    public int getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(int dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public int getDataSourceEventTypeId() {
        return dataSourceEventTypeId;
    }

    public void setDataSourceEventTypeId(int dataSourceEventTypeId) {
        this.dataSourceEventTypeId = dataSourceEventTypeId;
    }

    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(AlarmLevels alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getData()
     */
    @Override
    public EventType toEventType() {
        return new DataSourceEventType(dataSourceId, dataSourceEventTypeId, alarmLevel, duplicateHandling);
    }
}
