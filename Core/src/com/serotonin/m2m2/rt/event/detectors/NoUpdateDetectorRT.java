/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.NoUpdateDetectorVO;

/**
 * @author Matthew Lohbihler
 */
public class NoUpdateDetectorRT extends DifferenceDetectorRT<NoUpdateDetectorVO> {
    
	public NoUpdateDetectorRT(NoUpdateDetectorVO vo) {
        super(vo);
    }

    @Override
    public void pointUpdated(PointValueTime newValue) {
        pointData();
    }

    @Override
    public TranslatableMessage getMessage() {
        return new TranslatableMessage("event.detector.noUpdate", vo.getDataPoint().getExtendedName(),
                getDurationDescription());
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadNameImpl() {
		return "NoUpdate Detector " + this.vo.getXid();
	}

}
