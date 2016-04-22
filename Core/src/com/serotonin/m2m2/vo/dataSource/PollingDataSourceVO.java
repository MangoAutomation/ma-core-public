/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.util.SerializationHelper;

/**
 * Mid-layer class to assist with Polling style data sources
 * 
 * @author Terry Packer
 *
 */
public abstract class PollingDataSourceVO<T extends PollingDataSourceVO<T>> extends DataSourceVO<PollingDataSourceVO<T>>{

    @JsonProperty
    protected boolean quantize;
    
    protected String cronPattern;
    protected int updatePeriodType = Common.TimePeriods.MINUTES;
    protected int updatePeriods = 5;

    
    public boolean isQuantize() {
		return quantize;
	}

	public void setQuantize(boolean quantize) {
		this.quantize = quantize;
	}

	public String getCronPattern() {
		return cronPattern;
	}

	public void setCronPattern(String cronPattern) {
		this.cronPattern = cronPattern;
	}

	public int getUpdatePeriods() {
        return updatePeriods;
    }

    public void setUpdatePeriods(int updatePeriods) {
        this.updatePeriods = updatePeriods;
    }

    public int getUpdatePeriodType() {
        return updatePeriodType;
    }

    public void setUpdatePeriodType(int updatePeriodType) {
        this.updatePeriodType = updatePeriodType;
    }
    
    @Override
    public TranslatableMessage getConnectionDescription() {
    	if (!StringUtils.isBlank(cronPattern))
        	return new TranslatableMessage("common.default", cronPattern);
        else
            return Common.getPeriodDescription(updatePeriodType, updatePeriods);
    }
    
    @Override
    public void validate(ProcessResult response){
    	super.validate(response);
    	if (StringUtils.isBlank(cronPattern)){
            if (!Common.TIME_PERIOD_CODES.isValidId(updatePeriodType))
                response.addContextualMessage("updatePeriodType", "validate.invalidValue");
            if (updatePeriods <= 0)
                response.addContextualMessage("updatePeriods", "validate.greaterThanZero");
    	}else {
            try {
                new CronTimerTrigger(cronPattern);
            }
            catch (Exception e) {
                response.addContextualMessage("cronPattern", "validate.invalidCron", cronPattern);
            }
        }
    }
        
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeBoolean(quantize);
        SerializationHelper.writeSafeUTF(out, cronPattern);
        out.writeInt(updatePeriodType);
        out.writeInt(updatePeriods);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
        	quantize = in.readBoolean();
        	cronPattern = SerializationHelper.readSafeUTF(in);
        	updatePeriodType = in.readInt();
        	updatePeriods = in.readInt();
        }
        else {
            throw new ShouldNeverHappenException("Unknown serialization version.");
        }

    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        if (StringUtils.isBlank(cronPattern)){
        	writer.writeEntry("updatePeriods", updatePeriods);
        	writeUpdatePeriodType(writer, updatePeriodType);
        }else{
        	writer.writeEntry("cronPattern", cronPattern);
        }
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject json) throws JsonException {
    	super.jsonRead(reader, json);
    	if(json.containsKey("cronPattern")){
    		cronPattern = json.getString("cronPattern");
    	}else{
	    	Integer value = readUpdatePeriodType(json);
	        if (value != null)
	            updatePeriodType = value;
	        updatePeriods = json.getInt("updatePeriods", 5);
    	}
    }
  
}
