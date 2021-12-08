/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;
import java.util.EnumSet;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.DataTypes;
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
        super(vo, EnumSet.of(DataTypes.NUMERIC));
    }
    
    public double getRateOfChangeThreshold() {
        return rateOfChangeThreshold;
    }

    public void setRateOfChangeThreshold(double rateOfChangeThreshold) {
        this.rateOfChangeThreshold = rateOfChangeThreshold;
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
    public AbstractEventDetectorRT<RateOfChangeDetectorVO> createRuntime() {
        return new RateOfChangeDetectorRT(this);
    }
    
    @Override
    protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage comparison = getComparisonDescription();
        TranslatableMessage durationDesc = getDurationDescription();
        TranslatableMessage rateOfChangeDurationDesc = getRateOfChangeDurationDescription();
 
        if (calculationMode == CalculationMode.INSTANTANEOUS) {
            if(durationDesc == null)
                return new TranslatableMessage("event.detectorVo.rocInstantaneous", comparison);
            else
                return new TranslatableMessage("event.detectorVo.rocInstantaneousDuration", comparison, durationDesc);
        }else {
            if(durationDesc == null)
                return new TranslatableMessage("event.detectorVo.rocAverage", comparison, rateOfChangeDurationDesc);
            else
                return new TranslatableMessage("event.detectorVo.rocAverageDuration", comparison, rateOfChangeDurationDesc, durationDesc);
        }
    }
    
    public TranslatableMessage getRateOfChangeDurationDescription() {
        return Common.getPeriodDescription(rateOfChangePeriodType, rateOfChangePeriods);
    }
    
    public TranslatableMessage getUnitsDescription() {
        switch(this.rateOfChangeThresholdPeriodType) {
            case TimePeriods.MILLISECONDS:
                return new TranslatableMessage("dateAndTime.millisecond.per");
            case TimePeriods.SECONDS:
                return new TranslatableMessage("dateAndTime.second.per");
            case TimePeriods.MINUTES:
                return new TranslatableMessage("dateAndTime.minute.per");
            case TimePeriods.HOURS:
                return new TranslatableMessage("dateAndTime.hour.per");
            case TimePeriods.DAYS:
                return new TranslatableMessage("dateAndTime.day.per");
            case TimePeriods.WEEKS:
                return new TranslatableMessage("dateAndTime.week.per");
            case TimePeriods.MONTHS:
                return new TranslatableMessage("dateAndTime.month.per");
            case TimePeriods.YEARS:
                return new TranslatableMessage("dateAndTime.year.per");
            default:
                throw new ShouldNeverHappenException("Unsupported time period: " + rateOfChangeThresholdPeriodType);
        }

    }
    
    public TranslatableMessage getComparisonDescription() {
        switch(comparisonMode) {
            case GREATER_THAN:
                return new TranslatableMessage("event.detectorVo.roc.greaterThan", dataPoint.getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), getUnitsDescription());
            case GREATER_THAN_OR_EQUALS:
                return new TranslatableMessage("event.detectorVo.roc.greaterThanEqualTo", dataPoint.getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), getUnitsDescription());
            case LESS_THAN:
                return new TranslatableMessage("event.detectorVo.roc.lessThan", dataPoint.getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), getUnitsDescription());
            case LESS_THAN_OR_EQUALS:
            default:
                return new TranslatableMessage("event.detectorVo.roc.lessThanEqualTo", dataPoint.getTextRenderer().getText(rateOfChangeThreshold, TextRenderer.HINT_SPECIFIC), getUnitsDescription());
        }
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("rateOfChangeThresholdPeriodType", Common.TIME_PERIOD_CODES.getCode(rateOfChangeThresholdPeriodType));
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
