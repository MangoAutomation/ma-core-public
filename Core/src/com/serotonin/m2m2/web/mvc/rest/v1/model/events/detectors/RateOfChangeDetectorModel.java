/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorModel extends TimeoutDetectorModel<RateOfChangeDetectorVO> {

    public RateOfChangeDetectorModel(RateOfChangeDetectorVO data) {
        super(data);
    }
    
    public double getChange() {
        return this.data.getChange();
    }
    
    public void setChange(double change) {
        this.data.setChange(change);
    }

}
