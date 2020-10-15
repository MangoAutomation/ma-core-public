/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.List;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 * @author Jared Wiltshire
 */
public class DataPointWithEventDetectorsAndCache extends DataPointWithEventDetectors {
    private final List<PointValueTime> initialCache;

    public DataPointWithEventDetectorsAndCache(DataPointWithEventDetectors vo, List<PointValueTime> initialCache) {
        super(vo.getDataPoint(), vo.getEventDetectors());
        this.initialCache = initialCache;
    }

    public DataPointWithEventDetectorsAndCache(DataPointVO vo,
                                               List<AbstractPointEventDetectorVO> detectors, List<PointValueTime> initialCache) {
        super(vo, detectors);
        this.initialCache = initialCache;
    }

    public List<PointValueTime> getInitialCache() {
        return initialCache;
    }
}
