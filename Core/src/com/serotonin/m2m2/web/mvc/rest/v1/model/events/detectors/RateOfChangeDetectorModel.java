/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO.ComparisonMode;
import com.serotonin.m2m2.vo.event.detector.RateOfChangeDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorModel extends TimeoutDetectorModel<RateOfChangeDetectorVO> {

    public RateOfChangeDetectorModel(RateOfChangeDetectorVO data) {
        super(data);
    }
    
    public double getRateOfChangeThreshold() {
        return data.getRateOfChangeThreshold();
    }

    public void setRateOfChangeThreshold(double rateOfChangeThreshold) {
        this.data.setRateOfChangeThreshold(rateOfChangeThreshold);
    }

    public int getRateOfChangeDurationPeriods() {
        return data.getRateOfChangeDurationPeriods();
    }

    public void setRateOfChangeDurationPeriods(int rateOfChangeDurationPeriods) {
        this.data.setRateOfChangeDurationPeriods(rateOfChangeDurationPeriods);
    }

    public int getRateOfChangeDurationType() {
        return data.getRateOfChangeDurationType();
    }

    public void setRateOfChangeDurationType(int rateOfChangeDurationType) {
        this.data.setRateOfChangeDurationType(rateOfChangeDurationType);
    }

    public Double getResetThreshold() {
        return data.getResetThreshold();
    }

    public void setResetThreshold(Double resetThreshold) {
        this.data.setResetThreshold(resetThreshold);
    }

    public ComparisonMode getComparisonMode() {
        return data.getComparisonMode();
    }

    public void setComparisonMode(ComparisonMode comparisonMode) {
        this.data.setComparisonMode(comparisonMode);
    }

    public boolean isUseAbsoluteValue() {
        return this.data.isUseAbsoluteValue();
    }
    
    public void setUseAbsoluteValue(boolean useAbsoluteValue) {
        this.data.setUseAbsoluteValue(useAbsoluteValue);
    }
}
