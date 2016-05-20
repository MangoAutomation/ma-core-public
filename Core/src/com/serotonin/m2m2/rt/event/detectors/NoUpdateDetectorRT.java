/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.detectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.event.detector.NoUpdateDetectorVO;
import com.serotonin.timer.RejectedTaskReason;

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
        return new TranslatableMessage("event.detector.noUpdate", vo.njbGetDataPoint().getName(),
                getDurationDescription());
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return "NoUpdate Detector " + this.vo.getXid();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.TimeoutClient#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.rejectionHandler.rejectedHighPriorityTask(reason);
	}
}
