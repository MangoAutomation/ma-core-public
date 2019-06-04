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
import com.serotonin.m2m2.rt.event.detectors.RateOfChangeDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class RateOfChangeDetectorVO extends TimeoutDetectorVO<RateOfChangeDetectorVO> {

    public static enum ComparisonMode {
        GREATER_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN,
        LESS_THAN_OR_EQUALS
    }
    
    private static final long serialVersionUID = 1L;
    
    @JsonProperty
    private double rateOfChangeThreshold;
    private int rateOfChangeDurationPeriods;
    private int rateOfChangeDurationType = Common.TimePeriods.SECONDS;
    private Double resetThreshold;
    @JsonProperty
    private ComparisonMode comparisonMode;
    @JsonProperty
    private boolean useAbsoluteValue; 
    
    public RateOfChangeDetectorVO(DataPointVO vo) {
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

    public boolean isUseAbsoluteValue() {
        return useAbsoluteValue;
    }

    public void setUseAbsoluteValue(boolean useAbsoluteValue) {
        this.useAbsoluteValue = useAbsoluteValue;
    }

    @Override
    public void validate(ProcessResult response) {
        super.validate(response);

        if(comparisonMode == null) {
            response.addContextualMessage("comparisonMode", "validate.required");
            return;
        }
        
        if(resetThreshold != null) {
            if(comparisonMode == ComparisonMode.LESS_THAN && resetThreshold <= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.greaterThan", rateOfChangeThreshold);
            } else if(comparisonMode == ComparisonMode.LESS_THAN_OR_EQUALS && resetThreshold <= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.greaterThan", rateOfChangeThreshold);
            } else if(comparisonMode == ComparisonMode.GREATER_THAN && resetThreshold >= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.lessThan", rateOfChangeThreshold);
            } else if(comparisonMode == ComparisonMode.GREATER_THAN_OR_EQUALS && resetThreshold >= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.greaterThan", rateOfChangeThreshold);
            }
        }
        
        if (!Common.TIME_PERIOD_CODES.isValidId(rateOfChangeDurationType))
            response.addContextualMessage("rateOfChangeDurationType", "validate.invalidValue");
        if (rateOfChangeDurationPeriods < 0)
            response.addContextualMessage("rateOfChangeDurationPeriods", "validate.greaterThanZero");
    }
    
    @Override
    public AbstractEventDetectorRT<RateOfChangeDetectorVO> createRuntime() {
        return new RateOfChangeDetectorRT(this);
    }
    
    @Override
    protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage durationDesc = getDurationDescription();
        TranslatableMessage rateOfChangeDurationDesc = getRateOfChangeDurationDescription();
        if (comparisonMode == ComparisonMode.GREATER_THAN_OR_EQUALS) {
            //Check if Not above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangeNotLower", dataPoint
                        .getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), rateOfChangeDurationDesc);
            return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangeNotLowerPeriod", dataPoint
                        .getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), rateOfChangeDurationDesc, durationDesc);
        }
        else if(comparisonMode == ComparisonMode.LESS_THAN){
            //Must be above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.lowLimitRateOfChange", dataPoint.getTextRenderer()
                        .getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), rateOfChangeDurationDesc);
            return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangePeriod", dataPoint.getTextRenderer()
                        .getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), rateOfChangeDurationDesc, durationDesc);
        }else if (comparisonMode == ComparisonMode.LESS_THAN_OR_EQUALS) {
            //Check if Not above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.highLimitRateOfChangeNotHigher", dataPoint
                        .getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), rateOfChangeDurationDesc);
            return new TranslatableMessage("event.detectorVo.highLimitRateOfChangeNotHigherPeriod", dataPoint
                        .getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), rateOfChangeDurationDesc, durationDesc);
        }
        else {
            //Must be above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.highLimitRateOfChange", dataPoint.getTextRenderer()
                        .getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), rateOfChangeDurationDesc);
            return new TranslatableMessage("event.detectorVo.highLimitRateOfChangePeriod", dataPoint.getTextRenderer()
                        .getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), rateOfChangeDurationDesc, durationDesc);
        }
    }
    
    public TranslatableMessage getRateOfChangeDurationDescription() {
        return Common.getPeriodDescription(rateOfChangeDurationType, rateOfChangeDurationPeriods);
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
