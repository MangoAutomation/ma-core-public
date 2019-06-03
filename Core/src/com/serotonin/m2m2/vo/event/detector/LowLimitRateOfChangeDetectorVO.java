/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.LowLimitRateOfChangeDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class LowLimitRateOfChangeDetectorVO extends TimeoutDetectorVO<LowLimitRateOfChangeDetectorVO> {
    
    public static enum ComparisonMode {
        GREATER_THAN_OR_EQUALS,
        LESS_THAN
    }
    
    private static final long serialVersionUID = 1L;
    
    @JsonProperty
    private double rateOfChangeThreshold;
    private int rateOfChangeDurationPeriods;
    private int rateOfChangeDurationType = Common.TimePeriods.SECONDS;
    private Double resetThreshold;
    @JsonProperty
    private ComparisonMode comparisonMode;
    
    public LowLimitRateOfChangeDetectorVO(DataPointVO vo) {
        super(vo, new int[] {DataTypes.NUMERIC} );
    }
    
    public double getRateOfChangeThreshold() {
        return rateOfChangeThreshold;
    }

    public void setRateOfChangeThreshold(double rateOfChangeThreshold) {
        this.rateOfChangeThreshold = rateOfChangeThreshold;
    }

    public int getRateOfChangeDurationPeriods() {
        return rateOfChangeDurationPeriods;
    }

    public void setRateOfChangeDurationPeriods(int rateOfChangeDurationPeriods) {
        this.rateOfChangeDurationPeriods = rateOfChangeDurationPeriods;
    }

    public int getRateOfChangeDurationType() {
        return rateOfChangeDurationType;
    }

    public void setRateOfChangeDurationType(int rateOfChangeDurationType) {
        this.rateOfChangeDurationType = rateOfChangeDurationType;
    }

    public Double getResetThreshold() {
        return resetThreshold;
    }

    public void setResetThreshold(Double resetThreshold) {
        this.resetThreshold = resetThreshold;
    }

    public ComparisonMode getComparisonMode() {
        return comparisonMode;
    }

    public void setComparisonMode(ComparisonMode comparisonMode) {
        this.comparisonMode = comparisonMode;
    }
    
    @Override
    public void validate(ProcessResult response) {
        super.validate(response);

        if(comparisonMode == null) {
            response.addContextualMessage("comparisonMode", "validate.required");
            return;
        }
        
        if(resetThreshold != null) {
            if(comparisonMode == ComparisonMode.GREATER_THAN_OR_EQUALS && resetThreshold <= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.greaterThan", rateOfChangeThreshold);
            } else if(comparisonMode == ComparisonMode.LESS_THAN && resetThreshold >= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.lessThan", rateOfChangeThreshold);
            }
        }
        
        if (!Common.TIME_PERIOD_CODES.isValidId(rateOfChangeDurationType))
            response.addContextualMessage("rateOfChangeDurationType", "validate.invalidValue");
        if (rateOfChangeDurationPeriods < 0)
            response.addContextualMessage("rateOfChangeDurationPeriods", "validate.greaterThanZero");
    }
    
    @Override
    public AbstractEventDetectorRT<LowLimitRateOfChangeDetectorVO> createRuntime() {
        return new LowLimitRateOfChangeDetectorRT(this);
    }

    @Override
    protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage durationDesc = getDurationDescription();
        if (comparisonMode == ComparisonMode.GREATER_THAN_OR_EQUALS) {
            //Check if Not above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangeNotLower", dataPoint
                        .getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC));
            return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangeNotLowerPeriod", dataPoint
                        .getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else {
            //Must be above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.lowLimitRateOfChange", dataPoint.getTextRenderer()
                        .getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC));
            return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangePeriod", dataPoint.getTextRenderer()
                        .getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        if (resetThreshold != null)
            writer.writeEntry("resetThreshold", resetThreshold);
        writer.writeEntry("rateOfChangeDurationType", Common.TIME_PERIOD_CODES.getCode(rateOfChangeDurationType));
        writer.writeEntry("rateOfChangeDurationPeriods", rateOfChangeDurationPeriods);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        
        if (jsonObject.containsKey("resetThreshold"))
            resetThreshold = getDouble(jsonObject, "resetThreshold");
        
        String text = jsonObject.getString("rateOfChangeDurationType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.ped.missing", "rateOfChangeDurationType",
                    Common.TIME_PERIOD_CODES.getCodeList());

        rateOfChangeDurationType = Common.TIME_PERIOD_CODES.getId(text);
        if (!Common.TIME_PERIOD_CODES.isValidId(rateOfChangeDurationType))
            throw new TranslatableJsonException("emport.error.ped.invalid", "rocDurationType", text,
                    Common.TIME_PERIOD_CODES.getCodeList());

        rateOfChangeDurationPeriods = getInt(jsonObject, "rateOfChangeDurationPeriods");
    }

}
