/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.DataPointEventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;

public class DataPointEventType extends EventType {
    private int dataSourceId = -1;
    private int dataPointId;
    private int pointEventDetectorId;
    private int duplicateHandling = EventType.DuplicateHandling.IGNORE;

    public DataPointEventType() {
        // Required for reflection.
    }

    public DataPointEventType(int dataPointId, int pointEventDetectorId) {
        this.dataPointId = dataPointId;
        this.pointEventDetectorId = pointEventDetectorId;
    }

    public DataPointEventType(int dataSourceId, int dataPointId, int pointEventDetectorId, int duplicateHandling){
        this.dataSourceId = dataSourceId;
        this.dataPointId = dataPointId;
        this.pointEventDetectorId = pointEventDetectorId;
        this.duplicateHandling = duplicateHandling;
    }

    @Override
    public String getEventType() {
        return EventType.EventTypeNames.DATA_POINT;
    }

    @Override
    public String getEventSubtype() {
        return null;
    }

    @Override
    public int getDataSourceId() {
        if (dataSourceId == -1){
            DataPointVO vo = DataPointDao.instance.getDataPoint(dataPointId, false);
            if(vo != null) //In case the point has been deleted
                dataSourceId = vo.getDataSourceId();
        }
        return dataSourceId;
    }

    @Override
    public int getDataPointId() {
        return dataPointId;
    }

    public int getPointEventDetectorId() {
        return pointEventDetectorId;
    }

    @Override
    public String toString() {
        return "DataPointEventType(dataPointId=" + dataPointId + ", detectorId=" + pointEventDetectorId + ")";
    }

    @Override
    public int getDuplicateHandling() {
        return duplicateHandling;
    }

    public void setDuplicateHandling(int duplicateHandling) {
        this.duplicateHandling = duplicateHandling;
    }

    @Override
    public int getReferenceId1() {
        return dataPointId;
    }

    @Override
    public int getReferenceId2() {
        return pointEventDetectorId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + pointEventDetectorId;
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
        DataPointEventType other = (DataPointEventType) obj;
        if (pointEventDetectorId != other.pointEventDetectorId)
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
        dataPointId = getDataPointId(jsonObject, "dataPointXID");
        pointEventDetectorId = getPointEventDetectorId(jsonObject, dataPointId, "detectorXID");
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("dataPointXID", DataPointDao.instance.getXidById(dataPointId));
        writer.writeEntry("detectorXID", EventDetectorDao.instance.getXid(pointEventDetectorId));
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.type.EventType#asModel()
     */
    @Override
    public EventTypeModel asModel() {
        return new DataPointEventTypeModel(this);
    }

    @Override
    public boolean hasPermission(PermissionHolder user) {
        DataPointVO point = DataPointDao.instance.get(dataPointId);
        if(point == null)
            return false;
        return Permissions.hasDataPointReadPermission(user, point);
    }
}
