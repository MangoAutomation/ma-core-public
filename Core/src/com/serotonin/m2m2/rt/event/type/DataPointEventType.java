/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;
import java.util.Map;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyField;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

public class DataPointEventType extends EventType {
    private int dataSourceId = Common.NEW_ID;
    private int dataPointId;
    private int pointEventDetectorId;
    private DuplicateHandling duplicateHandling = DuplicateHandling.IGNORE;
    private LazyField<MangoPermission> readPermission;

    public DataPointEventType() {
        // Required for reflection.
    }

    public DataPointEventType(DataPointVO dataPoint, AbstractPointEventDetectorVO eventDetector) {
        this.dataPointId = dataPoint.getId();
        this.pointEventDetectorId = eventDetector.getId();
        supplyReference1(() -> {
            return dataPoint;
        });
        supplyReference2(() -> {
            return eventDetector;
        });
        this.readPermission = new LazyField<>(dataPoint.getReadPermission());
    }

    public DataPointEventType(int dataPointId, int pointEventDetectorId) {
        this.dataPointId = dataPointId;
        this.pointEventDetectorId = pointEventDetectorId;
        supplyReference1(() -> {
            return Common.getBean(DataPointDao.class).get(dataPointId);
        });
        supplyReference2(() -> {
            return Common.getBean(EventDetectorDao.class).get(pointEventDetectorId);
        });

        this.readPermission = new LazyField<>(() -> {
            Integer readPermissionId = Common.getBean(DataPointDao.class).getReadPermissionId(dataPointId);
            if(readPermissionId != null) {
                try {
                    return Common.getBean(PermissionService.class).get(readPermissionId);
                }catch(NotFoundException e) {
                    return MangoPermission.superadminOnly();
                }
            }else {
                return MangoPermission.superadminOnly();
            }
        });
    }

    public DataPointEventType(int dataSourceId, int dataPointId, int pointEventDetectorId, DuplicateHandling duplicateHandling){
        this(dataPointId, pointEventDetectorId);
        this.dataSourceId = dataSourceId;
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
        if (dataSourceId == Common.NEW_ID){
            DataPointVO vo = (DataPointVO)getReference1();
            if(vo != null) { //In case the point has been deleted
                dataSourceId = vo.getDataSourceId();
            }
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
    public DuplicateHandling getDuplicateHandling() {
        return duplicateHandling;
    }

    public void setDuplicateHandling(DuplicateHandling duplicateHandling) {
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
        dataPointId = 0;
        pointEventDetectorId = 0;
        String pointXid = jsonObject.getString("dataPointXID");
        String detectorXid = jsonObject.getString("detectorXID");
        if (pointXid != null) {
            dataPointId = getDataPointId(jsonObject, "dataPointXID");
            if (detectorXid != null) {
                pointEventDetectorId = getPointEventDetectorId(jsonObject, dataPointId, "detectorXID");
            }
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("dataPointXID", DataPointDao.getInstance().getXidById(dataPointId));
        writer.writeEntry("detectorXID", EventDetectorDao.getInstance().getXidById(pointEventDetectorId));
    }

    @Override
    public boolean hasPermission(PermissionHolder user, PermissionService service) {
        return service.hasEventsSuperadminViewPermission(user) || service.hasPermission(user, this.readPermission.get());
    }

    @Override
    public MangoPermission getEventPermission(Map<String, Object> context, PermissionService service) {
        DataPointVO dp = (DataPointVO)context.get(PointEventDetectorRT.DATA_POINT_CONTEXT_KEY);
        if(dp != null) {
            return dp.getReadPermission();
        }else {
            return MangoPermission.superadminOnly();
        }
    }
}
