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
    
    private static final long serialVersionUID = 1L;
    
    @JsonProperty
    private double change;
    private double resetChange;
    private boolean useResetChange;
    @JsonProperty
    private boolean notLower;
    protected int rocDuration;
    protected int rocDurationType = Common.TimePeriods.SECONDS;
    
    
    public LowLimitRateOfChangeDetectorVO(DataPointVO vo) {
        super(vo, new int[] {DataTypes.NUMERIC} );
    }
    
    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public double getResetChange() {
        return resetChange;
    }

    public void setResetChange(double resetChange) {
        this.resetChange = resetChange;
    }

    public boolean isUseResetChange() {
        return useResetChange;
    }

    public void setUseResetChange(boolean useResetChange) {
        this.useResetChange = useResetChange;
    }

    public boolean isNotLower() {
        return notLower;
    }

    public void setNotLower(boolean notLower) {
        this.notLower = notLower;
    }

    public int getRocDuration() {
        return rocDuration;
    }

    public void setRocDuration(int rocDuration) {
        this.rocDuration = rocDuration;
    }

    public int getRocDurationType() {
        return rocDurationType;
    }

    public void setRocDurationType(int rocDurationType) {
        this.rocDurationType = rocDurationType;
    }

    @Override
    public void validate(ProcessResult response) {
        super.validate(response);

        if(useResetChange) {
            if(!notLower && resetChange <= change) {
                response.addContextualMessage("resetChange", "validate.greaterThan", change);
            } else if(notLower && resetChange >= change) {
                response.addContextualMessage("resetChange", "validate.lessThan", change);
            }
        }
        
        if (!Common.TIME_PERIOD_CODES.isValidId(rocDurationType))
            response.addContextualMessage("rocDurationType", "validate.invalidValue");
        if (rocDuration < 0)
            response.addContextualMessage("rocDuration", "validate.greaterThanZero");
    }
    
    @Override
    public AbstractEventDetectorRT<LowLimitRateOfChangeDetectorVO> createRuntime() {
        return new LowLimitRateOfChangeDetectorRT(this);
    }

    @Override
    protected TranslatableMessage getConfigurationDescription() {
        TranslatableMessage durationDesc = getDurationDescription();
        if (notLower) {
            //Check if Not above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangeNotLower", dataPoint
                        .getTextRenderer().getText(change, TextRenderer.HINT_SPECIFIC));
            return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangeNotLowerPeriod", dataPoint
                        .getTextRenderer().getText(change, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else {
            //Must be above
            if (durationDesc == null)
                return new TranslatableMessage("event.detectorVo.lowLimitRateOfChange", dataPoint.getTextRenderer()
                        .getText(change, TextRenderer.HINT_SPECIFIC));
            return new TranslatableMessage("event.detectorVo.lowLimitRateOfChangePeriod", dataPoint.getTextRenderer()
                        .getText(change, TextRenderer.HINT_SPECIFIC), durationDesc);
        }

        
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        if (useResetChange)
            writer.writeEntry("resetChange", resetChange);
        writer.writeEntry("rocDurationType", Common.TIME_PERIOD_CODES.getCode(rocDurationType));
        writer.writeEntry("rocDuration", rocDuration);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        
        if (jsonObject.containsKey("resetChange")) {
            useResetChange = true;
            resetChange = getDouble(jsonObject, "resetChange");
        }
        
        String text = jsonObject.getString("rocDurationType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.ped.missing", "rocDurationType",
                    Common.TIME_PERIOD_CODES.getCodeList());

        rocDurationType = Common.TIME_PERIOD_CODES.getId(text);
        if (!Common.TIME_PERIOD_CODES.isValidId(rocDurationType))
            throw new TranslatableJsonException("emport.error.ped.invalid", "rocDurationType", text,
                    Common.TIME_PERIOD_CODES.getCodeList());

        rocDuration = getInt(jsonObject, "rocDuration");
    }

}
