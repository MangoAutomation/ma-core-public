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
import com.serotonin.m2m2.rt.event.detectors.SmoothnessDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class SmoothnessDetectorVO extends TimeoutDetectorVO<SmoothnessDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private double limit;
	@JsonProperty
	private double boxcar = 3;
	
	public SmoothnessDetectorVO(DataPointVO vo) {
		super(vo, EnumSet.of(DataType.NUMERIC));
	}

	public double getLimit() {
		return limit;
	}

	public void setLimit(double limit) {
		this.limit = limit;
	}

	public double getBoxcar() {
		return boxcar;
	}

	public void setBoxcar(double boxcar) {
		this.boxcar = boxcar;
	}

	@Override
	public AbstractEventDetectorRT<SmoothnessDetectorVO> createRuntime() {
		return new SmoothnessDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
		TranslatableMessage durationDesc = getDurationDescription();

        if (durationDesc == null)
            return new TranslatableMessage("event.detectorVo.smoothness", dataPoint.getTextRenderer().getText(
                    limit, TextRenderer.HINT_SPECIFIC));
        return new TranslatableMessage("event.detectorVo.smoothnessPeriod", dataPoint.getTextRenderer()
                    .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
	}

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
    	super.jsonWrite(writer);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    	super.jsonRead(reader, jsonObject);
    }
}
