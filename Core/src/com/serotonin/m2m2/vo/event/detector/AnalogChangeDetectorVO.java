/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.AnalogChangeDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * TODO Class is a work in progress, not wired in or tested yet.
 *      This will require uncommenting the line in the ModuleRegistry
 *      pertaining to this detector's definition
 * 
 * @author Terry Packer
 *
 */
public class AnalogChangeDetectorVO extends TimeoutDetectorVO<AnalogChangeDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	//Maximum change allowed before firing event
	@JsonProperty
	private double limit; 
	//Are we comparing to above or below limit
	@JsonProperty
	private boolean notHigher;

	public AnalogChangeDetectorVO() {
		super(new int[] { DataTypes.NUMERIC });
	}
	
	public double getLimit() {
		return limit;
	}

	public void setLimit(double limit) {
		this.limit = limit;
	}

	public boolean isNotHigher() {
		return notHigher;
	}

	public void setNotHigher(boolean notHigher) {
		this.notHigher = notHigher;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#createRuntime()
	 */
	@Override
	public AbstractEventDetectorRT<AnalogChangeDetectorVO> createRuntime() {
		return new AnalogChangeDetectorRT(this);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#getConfigurationDescription()
	 */
	@Override
	protected TranslatableMessage getConfigurationDescription() {
		TranslatableMessage message;
		TranslatableMessage durationDesc = getDurationDescription();
		
        if (notHigher) {
            //Check if Not above
            if (durationDesc == null)
                message = new TranslatableMessage("event.detectorVo.highLimitNotHigher", dataPoint
                        .getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC));
            else
                message = new TranslatableMessage("event.detectorVo.highLimitNotHigherPeriod", dataPoint
                        .getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else {
            //Must be above
            if (durationDesc == null)
                message = new TranslatableMessage("event.detectorVo.highLimit", dataPoint.getTextRenderer()
                        .getText(limit, TextRenderer.HINT_SPECIFIC));
            else
                message = new TranslatableMessage("event.detectorVo.highLimitPeriod", dataPoint.getTextRenderer()
                        .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        return message;

	}

}
