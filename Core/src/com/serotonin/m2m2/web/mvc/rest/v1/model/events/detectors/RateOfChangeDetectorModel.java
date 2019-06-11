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
    
    public int getRateOfChangeThresholdPeriodType() {
        return this.data.getRateOfChangeThresholdPeriodType();
    }

    public void setRateOfChangeThresholdPeriodType(int rateOfChangeThresholdPeriodType) {
        this.data.setRateOfChangeThresholdPeriodType(rateOfChangeThresholdPeriodType);
    }
    
    public int getRateOfChangePeriods() {
        return data.getRateOfChangePeriods();
    }

    public void setRateOfChangePeriods(int rateOfChangeDurationPeriods) {
        this.data.setRateOfChangePeriods(rateOfChangeDurationPeriods);
    }

    public int getRateOfChangePeriodType() {
        return data.getRateOfChangePeriodType();
    }

    public void setRateOfChangePeriodType(int rateOfChangeDurationType) {
        this.data.setRateOfChangePeriodType(rateOfChangeDurationType);
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
