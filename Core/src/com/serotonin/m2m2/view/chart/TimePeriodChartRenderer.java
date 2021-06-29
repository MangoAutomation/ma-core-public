/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.chart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

/**
 * Base Class for Time Period rendering Charts
 * 
 * @author Matthew Lohbihler, Terry Packer
 */
abstract public class TimePeriodChartRenderer extends BaseChartRenderer {
    
	private int timePeriod;
    @JsonProperty
    private int numberOfPeriods;
    
    /**
     * Convenience method for getting the start time of the chart period.
     */
    public long getStartTime() {
        return Common.timer.currentTimeMillis() - getDuration();
    }

    public void setStartTime(long t){
    	//NoOp
    }
    /**
     * Convenience method for getting the duration of the chart period.
     */
    public long getDuration() {
        return Common.getMillis(timePeriod, numberOfPeriods);
    }
    public void setDuration(long l){
    	//NoOp
    }
    public TimePeriodChartRenderer() {
        // no op
    }

    public TimePeriodChartRenderer(int timePeriod, int numberOfPeriods) {
        this.timePeriod = timePeriod;
        this.numberOfPeriods = numberOfPeriods;
    }

    public int getNumberOfPeriods() {
        return numberOfPeriods;
    }

    public void setNumberOfPeriods(int numberOfPeriods) {
        this.numberOfPeriods = numberOfPeriods;
    }

    public int getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(int timePeriod) {
        this.timePeriod = timePeriod;
    }

	//
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(timePeriod);
        out.writeInt(numberOfPeriods);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            timePeriod = in.readInt();
            numberOfPeriods = in.readInt();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("timePeriodType", Common.TIME_PERIOD_CODES.getCode(timePeriod));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);

        String text = jsonObject.getString("timePeriodType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.chart.missing", "timePeriodType",
                    Common.TIME_PERIOD_CODES.getCodeList());

        timePeriod = Common.TIME_PERIOD_CODES.getId(text);
        if (timePeriod == -1)
            throw new TranslatableJsonException("emport.error.chart.invalid", "timePeriodType", text,
                    Common.TIME_PERIOD_CODES.getCodeList());
    }
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.view.chart.ChartRenderer#validate(com.serotonin.m2m2.i18n.ProcessResult)
	 */
	@Override
	public void validate(ProcessResult result) {
		
		if(!Common.TIME_PERIOD_CODES.isValidId(timePeriod)){
			result.addContextualMessage("timePeriod", "validate.invalidValue");
		}
		
		if(numberOfPeriods < 1)
			result.addContextualMessage("numberOfPeriods", "validate.invalidValue");
	}
    
    
}
