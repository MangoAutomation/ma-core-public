/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.vo.dataPoint;

import java.util.List;
import java.util.stream.Collectors;

import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 *
 * Class used to transport a data point AND its detectors.  Used for Emport and Runtime
 *
 * @author Terry Packer
 */
public class DataPointWithEventDetectors {

    private final DataPointVO dataPoint;
    private List<AbstractPointEventDetectorVO> eventDetectors;


    public DataPointWithEventDetectors(DataPointVO vo,
            List<AbstractPointEventDetectorVO> detectors) {
        this.dataPoint = vo;
        this.eventDetectors = detectors;
    }

    public DataPointVO getDataPoint() {
        return dataPoint;
    }

    public List<AbstractPointEventDetectorVO> getEventDetectors() {
        return eventDetectors;
    }

    public void setEventDetectors(List<AbstractPointEventDetectorVO> eventDetectors) {
        this.eventDetectors = eventDetectors;
    }

    /**
     * Either add this as a new detector or replace an existing one based
     *  on matching XIDs.  Used during import of points and detectors
     *  to ensure that the last added detector is saved back to the point.
     */
    public void addOrReplaceDetector(AbstractPointEventDetectorVO dped) {
        eventDetectors.removeIf(d -> d.getXid().equals(dped.getXid()));
        eventDetectors.add(dped);
    }

    @Override
    public String toString() {
        String detectorNames = eventDetectors.stream().map(AbstractVO::getName).collect(Collectors.joining(","));
        return dataPoint.getName() + "[" + detectorNames + "]";
    }

}
