/*
    Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.List;

import com.serotonin.m2m2.db.dao.EnhancedPointValueDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.timer.AbstractTimer;

public class EnhancedPointValueCache extends PointValueCache {
    private final DataPointVO dataPoint;
    private final DataSourceVO<?> dataSource;
    private static final EnhancedPointValueDao enhancedDao = (EnhancedPointValueDao)PointValueCache.dao; //See PointValueCache.dao

    public EnhancedPointValueCache(DataPointVO dataPoint, DataSourceVO<?> dataSource, int defaultSize, List<PointValueTime> cache, AbstractTimer timer) {
        super(dataPoint.getId(), defaultSize, cache, timer);
        this.dataPoint = dataPoint;
        this.dataSource = dataSource;
    }
    
    @Override
    void savePointValueAsync(PointValueTime pvt, SetPointSource source) {
        enhancedDao.savePointValueAsync(dataPoint, dataSource, pvt, source, valueSavedCallback);
    }
    
    @Override
    PointValueTime savePointValueSync(PointValueTime pvt, SetPointSource source) {
        return enhancedDao.savePointValueSync(dataPoint, dataSource, pvt, source);
    }
}
