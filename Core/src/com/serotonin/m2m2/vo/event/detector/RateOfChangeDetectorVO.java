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
    
    public enum CalculationMode {
        INSTANTANEOUS,
        AVERAGE
    }
    
    private static final long serialVersionUID = 1L;
    
    @JsonProperty
    private double rateOfChangeThreshold;
    private int rateOfChangeThresholdPeriods;
    private int rateOfChangeThresholdPeriodType;
    private int rateOfChangePeriods;
    private int rateOfChangePeriodType = Common.TimePeriods.SECONDS;
    @JsonProperty
    private ComparisonMode comparisonMode;
    @JsonProperty
    private CalculationMode calculationMode;
    private double resetThreshold;
    private boolean useResetThreshold;
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
    
    public int getRateOfChangeThresholdPeriods() {
        return rateOfChangeThresholdPeriods;
    }

    public void setRateOfChangeThresholdPeriods(int rateOfChangeThresholdPeriods) {
        this.rateOfChangeThresholdPeriods = rateOfChangeThresholdPeriods;
    }

    public int getRateOfChangeThresholdPeriodType() {
        return rateOfChangeThresholdPeriodType;
    }

    public void setRateOfChangeThresholdPeriodType(int rateOfChangeThresholdPeriodType) {
        this.rateOfChangeThresholdPeriodType = rateOfChangeThresholdPeriodType;
    }

    public double getResetThreshold() {
        return resetThreshold;
    }

    public void setResetThreshold(double resetThreshold) {
        this.resetThreshold = resetThreshold;
    }

    public boolean isUseResetThreshold() {
        return useResetThreshold;
    }

    public void setUseResetThreshold(boolean useResetThreshold) {
        this.useResetThreshold = useResetThreshold;
    }

    public int getRateOfChangePeriods() {
        return rateOfChangePeriods;
    }

    public void setRateOfChangePeriods(int rateOfChangePeriods) {
        this.rateOfChangePeriods = rateOfChangePeriods;
    }

    public int getRateOfChangePeriodType() {
        return rateOfChangePeriodType;
    }

    public void setRateOfChangePeriodType(int rateOfChangePeriodType) {
        this.rateOfChangePeriodType = rateOfChangePeriodType;
    }

    public ComparisonMode getComparisonMode() {
        return comparisonMode;
    }

    public void setComparisonMode(ComparisonMode comparisonMode) {
        this.comparisonMode = comparisonMode;
    }

    public CalculationMode getCalculationMode() {
        return calculationMode;
    }

    public void setCalculationMode(CalculationMode calculationMode) {
        this.calculationMode = calculationMode;
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
        
        if(calculationMode == null) {
            response.addContextualMessage("calculationMode", "validate.required");
            return;
        }
        
        if(calculationMode == CalculationMode.AVERAGE && rateOfChangePeriods == 0)
            response.addContextualMessage("rateOfChangePeriods", "validate.greaterThanZero");
        else if(calculationMode == CalculationMode.INSTANTANEOUS && rateOfChangePeriods != 0)
            response.addContextualMessage("rateOfChangePeriods", "validate.invalidValue");
            
        
        if(useResetThreshold) {
            if(comparisonMode == ComparisonMode.LESS_THAN && resetThreshold <= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.greaterThan", rateOfChangeThreshold);
            } else if(comparisonMode == ComparisonMode.LESS_THAN_OR_EQUALS && resetThreshold <= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.greaterThan", rateOfChangeThreshold);
            } else if(comparisonMode == ComparisonMode.GREATER_THAN && resetThreshold >= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.lessThan", rateOfChangeThreshold);
            } else if(comparisonMode == ComparisonMode.GREATER_THAN_OR_EQUALS && resetThreshold >= rateOfChangeThreshold) {
                response.addContextualMessage("resetThreshold", "validate.lessThan", rateOfChangeThreshold);
            }
        }
        
        if(rateOfChangeThresholdPeriods < 1)
            response.addContextualMessage("rateOfChangeThresholdPeriods", "validate.greaterThanZero");
        
        if (!Common.TIME_PERIOD_CODES.isValidId(rateOfChangeThresholdPeriodType))
            response.addContextualMessage("rateOfChangeThresholdPeriodType", "validate.invalidValue");
        
        if (!Common.TIME_PERIOD_CODES.isValidId(rateOfChangePeriodType))
            response.addContextualMessage("rateOfChangePeriodType", "validate.invalidValue");
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
        return Common.getPeriodDescription(rateOfChangePeriodType, rateOfChangePeriods);
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("rateOfChangeThresholdPeriodType", Common.TIME_PERIOD_CODES.getCode(rateOfChangeThresholdPeriodType));
        writer.writeEntry("rateOfChangeThresholdPeriods", rateOfChangeThresholdPeriods);
        if (useResetThreshold)
            writer.writeEntry("resetThreshold", resetThreshold);
        writer.writeEntry("rateOfChangePeriodType", Common.TIME_PERIOD_CODES.getCode(rateOfChangePeriodType));
        writer.writeEntry("rateOfChangePeriods", rateOfChangePeriods);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        
        String text = jsonObject.getString("rateOfChangeThresholdPeriodType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.ped.missing", "rateOfChangeThresholdPeriodType",
                    Common.TIME_PERIOD_CODES.getCodeList());
        rateOfChangeThresholdPeriodType = Common.TIME_PERIOD_CODES.getId(text);
        if (!Common.TIME_PERIOD_CODES.isValidId(rateOfChangeThresholdPeriodType))
            throw new TranslatableJsonException("emport.error.ped.invalid", "rateOfChangeThresholdPeriodType", text,
                    Common.TIME_PERIOD_CODES.getCodeList());
        
        if (jsonObject.containsKey("resetThreshold")) {
            resetThreshold = getDouble(jsonObject, "resetThreshold");
            useResetThreshold = true;
        }
        rateOfChangeThresholdPeriods = getInt(jsonObject, "rateOfChangeThresholdPeriods");
        
        text = jsonObject.getString("rateOfChangePeriodType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.ped.missing", "rateOfChangePeriodType",
                    Common.TIME_PERIOD_CODES.getCodeList());
        rateOfChangePeriodType = Common.TIME_PERIOD_CODES.getId(text);
        if (!Common.TIME_PERIOD_CODES.isValidId(rateOfChangePeriodType))
            throw new TranslatableJsonException("emport.error.ped.invalid", "rateOfChangePeriodType", text,
                    Common.TIME_PERIOD_CODES.getCodeList());

        rateOfChangePeriods = getInt(jsonObject, "rateOfChangePeriods");
    }
}
