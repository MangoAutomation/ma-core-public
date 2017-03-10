/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
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
    
    private int rollup;
    private int rollupPeriodType; 
    @JsonProperty
    private int rollupPeriods;
    private int relativeDateType;

    /**
     * Convenience method for getting the start time of the chart period.
     */
    public long getStartTime() {
        return System.currentTimeMillis() - getDuration();
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

    public int getRollup() {
		return rollup;
	}

	public void setRollup(int rollup) {
		this.rollup = rollup;
	}

	public int getRollupPeriodType() {
		return rollupPeriodType;
	}

	public void setRollupPeriodType(int rollupPeriodType) {
		this.rollupPeriodType = rollupPeriodType;
	}

	public int getRollupPeriods() {
		return rollupPeriods;
	}

	public void setRollupPeriods(int rollupPeriods) {
		this.rollupPeriods = rollupPeriods;
	}

	public int getRelativeDateType() {
		return relativeDateType;
	}

	public void setRelativeDateType(int relativeDateType) {
		this.relativeDateType = relativeDateType;
	}

	//
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(timePeriod);
        out.writeInt(numberOfPeriods);
        out.writeInt(rollup);
        out.writeInt(rollupPeriodType);
        out.writeInt(rollupPeriods);
        out.writeInt(relativeDateType);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            timePeriod = in.readInt();
            numberOfPeriods = in.readInt();
            rollup = Common.Rollups.NONE;
            rollupPeriodType = Common.TimePeriods.HOURS;
            rollupPeriods = 1;
            relativeDateType = Common.RelativeDateTypes.PAST;
        }else if(ver == 2){
            timePeriod = in.readInt();
            numberOfPeriods = in.readInt();
            rollup = in.readInt();
            rollupPeriodType = in.readInt();
            rollupPeriods = in.readInt();
            relativeDateType = in.readInt();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("timePeriodType", Common.TIME_PERIOD_CODES.getCode(timePeriod));
        writer.writeEntry("rollup", Common.ROLLUP_CODES.getCode(rollup));
        writer.writeEntry("rollupPeriodType", Common.TIME_PERIOD_CODES.getCode(rollupPeriodType));
        writer.writeEntry("relativeDateType", Common.RELATIVE_DATE_TYPE_CODES.getCode(relativeDateType));
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
        
        //Rollup
        text = jsonObject.getString("rollup");
        if (text != null){
	        rollup = Common.ROLLUP_CODES.getId(text);
	        if (rollup == -1)
	            throw new TranslatableJsonException("emport.error.chart.invalid", "rollup", text,
	                    Common.ROLLUP_CODES.getCodeList());
        }
        //Rollup Period Type
        text = jsonObject.getString("rollupPeriodType");
        if (text != null){
	        rollupPeriodType = Common.TIME_PERIOD_CODES.getId(text);
	        if (rollupPeriodType == -1)
	            throw new TranslatableJsonException("emport.error.chart.invalid", "rollupPeriodType", text,
	                    Common.TIME_PERIOD_CODES.getCodeList());
        }
        //Relative Date type
        text = jsonObject.getString("relativeDateType");
        if (text != null){

        	relativeDateType = Common.RELATIVE_DATE_TYPE_CODES.getId(text);
        if (relativeDateType == -1)
            throw new TranslatableJsonException("emport.error.chart.invalid", "relativeDateType", text,
                    Common.RELATIVE_DATE_TYPE_CODES.getCodeList());
        }
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
	
		//Rollup
		if(!Common.ROLLUP_CODES.isValidId(rollup))
			result.addContextualMessage("rollup", "validate.invalidValue");
		//Rollup Period Type
		if(!Common.TIME_PERIOD_CODES.isValidId(rollupPeriodType))
			result.addContextualMessage("rollupPeriodType", "validate.invalidValue");
		//Rollup Periods
		if(rollupPeriods < 1)
			result.addContextualMessage("rollupPeriods", "validate.invalidValue");
		//Relative Date Type
		if(!Common.RELATIVE_DATE_TYPE_CODES.isValidId(relativeDateType))
			result.addContextualMessage("relativeDateType", "validate.invalidValue");
	}
    
    
}
