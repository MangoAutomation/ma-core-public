/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.RateOfChangeDetectorRT;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorVO extends TimeoutDetectorVO<RateOfChangeDetectorVO> {
    
    private static final long serialVersionUID = 1L;
    
    @JsonProperty
    private double change;
    
    public RateOfChangeDetectorVO(DataPointVO vo) {
        super(vo, new int[] {DataTypes.NUMERIC} );
    }
    
    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    //TODO Validation
    
    @Override
    public AbstractEventDetectorRT<RateOfChangeDetectorVO> createRuntime() {
        return new RateOfChangeDetectorRT(this);
    }

    @Override
    protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage durationDesc = getDurationDescription();
        if (durationDesc == null)
            return new TranslatableMessage("event.detectorVo.rateOfChange");
        else
            return new TranslatableMessage("event.detectorVo.rateOfChangePeriod", durationDesc);
    }

}
