/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.AnalogLowLimitDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class AnalogLowLimitDetectorVO extends TimeoutDetectorVO<AnalogLowLimitDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private double limit;
	private double resetLimit;
	private boolean useResetLimit;
	@JsonProperty
	private boolean notLower;

	public AnalogLowLimitDetectorVO(DataPointVO vo) {
		super(vo, new int[] { DataTypes.NUMERIC });
	}

	public double getLimit() {
		return limit;
	}

	public void setLimit(double limit) {
		this.limit = limit;
	}

	public double getResetLimit() {
		return resetLimit;
	}

	public void setResetLimit(double resetLimit) {
		this.resetLimit = resetLimit;
	}
	
	public boolean isUseResetLimit() {
		return useResetLimit;
	}

	public void setUseResetLimit(boolean useResetLimit) {
		this.useResetLimit = useResetLimit;
	}
	
	public boolean isNotLower() {
		return notLower;
	}

	public void setNotLower(boolean notLower) {
		this.notLower = notLower;
	}

	@Override
	public AbstractEventDetectorRT<AnalogLowLimitDetectorVO> createRuntime() {
		return new AnalogLowLimitDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
		TranslatableMessage durationDesc = getDurationDescription();
		
        if (notLower) {
            //Not below
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.lowLimitNotLower", dataPoint.getTextRenderer()
                        .getText(limit, TextRenderer.HINT_SPECIFIC));
            return new TranslatableMessage("event.detectorVo.lowLimitNotLowerPeriod", dataPoint
                        .getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else {
            //Must be below
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.lowLimit", dataPoint.getTextRenderer().getText(
                        limit, TextRenderer.HINT_SPECIFIC));
            return new TranslatableMessage("event.detectorVo.lowLimitPeriod", dataPoint.getTextRenderer()
                        .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
	}
	
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
    	super.jsonWrite(writer);
        if (useResetLimit)
            writer.writeEntry("resetLimit", resetLimit);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    	super.jsonRead(reader, jsonObject);
        
        if (jsonObject.containsKey("resetLimit")) {
        	useResetLimit = true;
            resetLimit = getDouble(jsonObject, "resetLimit");
        }
    }
    
}
