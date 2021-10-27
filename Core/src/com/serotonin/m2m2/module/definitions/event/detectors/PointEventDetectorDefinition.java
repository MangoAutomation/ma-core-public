/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.db.tables.EventDetectors;
import com.infiniteautomation.mango.spring.service.DataPointService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public abstract class PointEventDetectorDefinition<T extends AbstractPointEventDetectorVO> extends EventDetectorDefinition<T> {

    @Autowired
    private DataPointService dataPointService;

    @Override
    public Field<Integer> getSourceIdColumnName() {
        return EventDetectors.EVENT_DETECTORS.dataPointId;
    }

    @Override
    public String getSourceTypeName() {
        return EventType.EventTypeNames.DATA_POINT;
    }

    /**
     * The right way to create a point event detector
     * @param dp
     * @return
     */
    public T baseCreateEventDetectorVO(DataPointVO dp) {
        T detector = createEventDetectorVO(dp);
        detector.setDefinition(this);
        return detector;
    }

    @Override
    public void restartSource(T vo) {
        if(Common.runtimeManager.isDataPointRunning(vo.getSourceId())) {
            dataPointService.reloadDataPoint(vo.getSourceId());
        }
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        if(vo.getDataPoint() == null)
            return false;
        return dataPointService.hasEditPermission(user, vo.getDataPoint());
    }

    @Override
    protected T createEventDetectorVO(int sourceId) {
        return createEventDetectorVO(DataPointDao.getInstance().get(sourceId));
    }

    /**
     * Implement in concrete classes
     * @param dp
     * @return
     */
    protected abstract T createEventDetectorVO(DataPointVO dp);

    @Override
    public void validate(ProcessResult response, T vo) {

        //We currently don't check to see if the point exists
        // because of SQL constraints

        //Ensure the point is at least not null
        if(vo.getDataPoint() == null)
            response.addContextualMessage("sourceId", "validate.invalidValue");
        else {
            //Is valid data type
            boolean valid = false;
            for(int type : vo.getSupportedDataTypes()){
                if(type == vo.getDataPoint().getPointLocator().getDataTypeId()){
                    valid = true;
                    break;
                }
            }
            if(!valid){
                //Add message
                response.addContextualMessage("dataPoint.pointLocator.dataType", "validate.invalidValue");
            }
        }

        //Is valid alarm level
        if (vo.getAlarmLevel() == null)
            response.addContextualMessage("alarmLevel", "validate.invalidValue");

    }

}
