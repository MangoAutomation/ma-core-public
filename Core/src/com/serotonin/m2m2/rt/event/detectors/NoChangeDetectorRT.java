/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.NoChangeDetectorVO;

/**
 * @author Matthew Lohbihler
 */
public class NoChangeDetectorRT extends DifferenceDetectorRT<NoChangeDetectorVO> {
    
	public NoChangeDetectorRT(NoChangeDetectorVO vo) {
        super(vo);
    }

    @Override
    public void pointChanged(PointValueTime oldValue, PointValueTime newValue) {
        pointData(Common.timer.currentTimeMillis());
    }

    @Override
    public TranslatableMessage getMessage() {
        return new TranslatableMessage("event.detector.noChange", vo.getDataPoint().getExtendedName(),
                getDurationDescription());
    }
    
	@Override
	public String getThreadNameImpl() {
		return "NoChange Detector " + this.vo.getXid();
	}

}
