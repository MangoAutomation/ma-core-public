/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;
import java.util.EnumSet;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.AnalogHighLimitDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class AnalogHighLimitDetectorVO extends TimeoutDetectorVO<AnalogHighLimitDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private double limit;
	private double resetLimit;
	private boolean useResetLimit;
	@JsonProperty
	private boolean notHigher;
	
	public AnalogHighLimitDetectorVO(DataPointVO vo) {
		super(vo, EnumSet.of(DataType.NUMERIC));
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
	
	public boolean isNotHigher() {
		return notHigher;
	}

	public void setNotHigher(boolean notHigher) {
		this.notHigher = notHigher;
	}

	@Override
	public AbstractEventDetectorRT<AnalogHighLimitDetectorVO> createRuntime() {
		return new AnalogHighLimitDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
		TranslatableMessage durationDesc = getDurationDescription();
		
        if (notHigher) {
            //Check if Not above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.highLimitNotHigher", dataPoint
                        .getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC));
            return new TranslatableMessage("event.detectorVo.highLimitNotHigherPeriod", dataPoint
                        .getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else {
            //Must be above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.highLimit", dataPoint.getTextRenderer()
                        .getText(limit, TextRenderer.HINT_SPECIFIC));
            return new TranslatableMessage("event.detectorVo.highLimitPeriod", dataPoint.getTextRenderer()
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
