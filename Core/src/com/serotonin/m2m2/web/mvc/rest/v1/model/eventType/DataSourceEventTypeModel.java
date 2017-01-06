/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * 
 * @author Terry Packer
 */
public class DataSourceEventTypeModel extends EventTypeModel{

    private int dataSourceId;
    private int dataSourceEventTypeId;
    private int alarmLevel;
    private int duplicateHandling;
	
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
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getDuplicateHandling()
	 */
	@Override
	public int getDuplicateHandling() {
		return this.duplicateHandling;
	}
	
	public void setDuplicateHandling(int duplicateHandling){
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

	public int getAlarmLevel() {
		return alarmLevel;
	}

	public void setAlarmLevel(int alarmLevel) {
		this.alarmLevel = alarmLevel;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getData()
	 */
	@Override
	public EventType getEventTypeInstance() {
		return new DataSourceEventType(dataSourceId, dataSourceEventTypeId, alarmLevel, duplicateHandling);
	}
}
