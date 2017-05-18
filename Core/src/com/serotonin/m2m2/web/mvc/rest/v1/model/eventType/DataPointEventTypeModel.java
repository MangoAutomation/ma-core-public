/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * 
 * @author Terry Packer
 */
public class DataPointEventTypeModel extends EventTypeModel{

    private int dataSourceId = -1;
    private int dataPointId;
    private int pointEventDetectorId;
    private int duplicateHandling = EventType.DuplicateHandling.IGNORE;

	
	public DataPointEventTypeModel(){ }
	
	
	public DataPointEventTypeModel(DataPointEventType type){
		this.dataSourceId = type.getDataSourceId();
		this.dataPointId = type.getDataPointId();
		this.pointEventDetectorId = type.getPointEventDetectorId();
		this.duplicateHandling = type.getDuplicateHandling();
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getTypeName()
	 */
	@Override
	public String getTypeName() {
		return EventType.EventTypeNames.DATA_POINT;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#isRateLimited()
	 */
	@Override
	public boolean isRateLimited() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel#getEventTypeInstance()
	 */
	@Override
	public EventType getEventTypeInstance() {
		return new DataPointEventType(dataSourceId, dataPointId, pointEventDetectorId, duplicateHandling);
	}

	public int getDataSourceId() {
		return dataSourceId;
	}

	public void setDataSourceId(int dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public int getDataPointId() {
		return dataPointId;
	}

	public void setDataPointId(int dataPointId) {
		this.dataPointId = dataPointId;
	}

	public int getPointEventDetectorId() {
		return pointEventDetectorId;
	}

	public void setPointEventDetectorId(int pointEventDetectorId) {
		this.pointEventDetectorId = pointEventDetectorId;
	}

	public String getDuplicateHandling() {
		return EventType.DUPLICATE_HANDLING_CODES.getCode(duplicateHandling);
	}

	public void setDuplicateHandling(int duplicateHandling) {
		this.duplicateHandling = EventType.DUPLICATE_HANDLING_CODES.getId(duplicateHandling);
	}
}
