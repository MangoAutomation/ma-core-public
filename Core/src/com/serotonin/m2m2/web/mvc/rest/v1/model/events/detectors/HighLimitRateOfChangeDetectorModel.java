/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.HighLimitRateOfChangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class HighLimitRateOfChangeDetectorModel extends TimeoutDetectorModel<HighLimitRateOfChangeDetectorVO> {

    public HighLimitRateOfChangeDetectorModel(HighLimitRateOfChangeDetectorVO data) {
        super(data);
    }
    
    public double getChange() {
        return this.data.getChange();
    }
    
    public void setChange(double change) {
        this.data.setChange(change);
    }
    
    public double getResetChange() {
        return this.data.getResetChange();
    }

    public void setResetChange(double resetChange) {
        this.data.setResetChange(resetChange);
    }

    public boolean isUseResetChange() {
        return this.data.isUseResetChange();
    }

    public void setUseResetChange(boolean useResetChange) {
        this.data.setUseResetChange(useResetChange);
    }

    public boolean isNotHigher() {
        return this.data.isNotHigher();
    }

    public void setNotHigher(boolean notHigher) {
        this.data.setNotHigher(notHigher);
    }

    public int getRocDuration() {
        return this.data.getRocDuration();
    }

    public void setRocDuration(int rocDuration) {
        this.data.setRocDuration(rocDuration);
    }

    public int getRocDurationType() {
        return this.data.getRocDurationType();
    }

    public void setRocDurationType(int rocDurationType) {
        this.data.setRocDurationType(rocDurationType);
    }

}
