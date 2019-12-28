/*
   Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
   @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.detectors;

import java.util.Objects;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
public abstract class PointEventDetectorDefinition<T extends AbstractPointEventDetectorVO<T>> extends EventDetectorDefinition<T>{

	public static final String SOURCE_ID_COLUMN_NAME = "dataPointId";
	
	private final LazyInitSupplier<PermissionService> permissionService = new LazyInitSupplier<>(() -> {
	       return Common.getBean(PermissionService.class); 
	    });
	
	@Override
	public String getSourceIdColumnName() {
		return SOURCE_ID_COLUMN_NAME;
	}
	
	@Override
	public String getSourceTypeName() {
		return EventType.EventTypeNames.DATA_POINT;
	}
	
	/**
	 * The right way to create a point event detector
	 * @param vo
	 * @return
	 */
    public <X extends AbstractEventDetectorVO<X>> T baseCreateEventDetectorVO(DataPointVO dp) {
        T detector = createEventDetectorVO(dp);
        detector.setDefinition(this);
        return detector;
    }
    
    @Override
    public void restartSource(T vo) {
        if(Common.runtimeManager.isDataPointRunning(vo.getSourceId())) {
            //Get full to ensure the event detectors are loaded
            DataPointVO dp = DataPointDao.getInstance().get(vo.getSourceId(), true);
            Objects.requireNonNull(dp, "No data point source set");
            Common.runtimeManager.restartDataPoint(dp);
        }
    }
    
    @Override
    public boolean hasEditPermission(PermissionHolder user, T vo) {
        if(vo.getDataPoint() == null)
            return false;
        return permissionService.get().hasDataSourcePermission(user, vo.getDataPoint().getDataSourceId());
    }
    
    @Override
    public boolean hasReadPermission(PermissionHolder user, T vo) {
        if(vo.getDataPoint() == null)
            return false;
        return permissionService.get().hasDataPointReadPermission(user, vo.getDataPoint());
    }
    
    @Override
    public boolean hasCreatePermission(PermissionHolder user, T vo) {
        if(vo.getDataPoint() == null)
            return false;
        return permissionService.get().hasDataSourcePermission(user, vo.getDataPoint().getDataSourceId());
    }
    
    @Override
    protected T createEventDetectorVO(int sourceId) {
        return createEventDetectorVO(DataPointDao.getInstance().get(sourceId, true));
    }
    
    /**
     * Implement in concrete classes
     * @param dp
     * @return
     */
    protected abstract T createEventDetectorVO(DataPointVO dp);

}
