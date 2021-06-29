/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.AnalogChangeDetectorRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;

/** * 
 * TODO Class is a work in progress, not wired in or tested yet.
 *      This will require uncommenting the line in the ModuleRegistry
 *      pertaining to this detector's definition
 * 
 * @author Terry Packer
 *
 */
public class AnalogChangeDetectorVO extends TimeoutDetectorVO<AnalogChangeDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	public interface UpdateEventType {
	    int CHANGES_ONLY = 1;
        int LOGGED_ONLY = 2;
	}
	
	public static final ExportCodes UPDATE_EVENT_TYPE_CODES = new ExportCodes();
    static{
        UPDATE_EVENT_TYPE_CODES.addElement(UpdateEventType.CHANGES_ONLY, "CHANGES_ONLY", "publisherEdit.publishType.changesOnly");   
        UPDATE_EVENT_TYPE_CODES.addElement(UpdateEventType.LOGGED_ONLY, "LOGGED_ONLY", "publisherEdit.publishType.loggedOnly");  
    }
	
	//Maximum change allowed before firing event
	@JsonProperty
	private double limit; 
	@JsonProperty
	private boolean checkIncrease = true;
	@JsonProperty
    private boolean checkDecrease = true;
	private int updateEvent = UpdateEventType.LOGGED_ONLY;

	public AnalogChangeDetectorVO(DataPointVO vo) {
		super(vo, new int[] { DataTypes.NUMERIC });
	}
	
	public double getLimit() {
		return limit;
	}

	public void setLimit(double limit) {
		this.limit = limit;
	}

	public boolean isCheckIncrease() {
		return checkIncrease;
	}

	public void setCheckIncrease(boolean checkIncrease) {
		this.checkIncrease = checkIncrease;
	}
	
	public boolean isCheckDecrease() {
        return checkDecrease;
    }

    public void setCheckDecrease(boolean checkDecrease) {
        this.checkDecrease = checkDecrease;
    }
    
    public int getUpdateEvent() {
        return updateEvent;
    }
    
    public void setUpdateEvent(int updateEvent) {
        this.updateEvent = updateEvent;
    }
    
    @Override
    public boolean isRtnApplicable() {
        return getDuration() != 0;
    }
	
	@Override
	public AbstractEventDetectorRT<AnalogChangeDetectorVO> createRuntime() {
		return new AnalogChangeDetectorRT(this);
	}

	@Override
	protected TranslatableMessage getConfigurationDescription() {
	    String prettyLimit = dataPoint.getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC);
		TranslatableMessage durationDescription = getDurationDescription();
		
		if(durationDescription != null) {
    		if(checkIncrease && checkDecrease)
                return new TranslatableMessage("event.detectorVo.analogChangePeriod", prettyLimit, durationDescription);
            else if(checkIncrease)
                return new TranslatableMessage("event.detectorVo.analogIncreasePeriod", prettyLimit, durationDescription);
            else if(checkDecrease)
                return new TranslatableMessage("event.detectorVo.analogDecreasePeriod", prettyLimit, durationDescription);
            else
                throw new ShouldNeverHappenException("Illegal state for analog change detector" + xid);
		} else {
		    if(checkIncrease && checkDecrease)
                return new TranslatableMessage("event.detectorVo.analogChange", prettyLimit);
            else if(checkIncrease)
                return new TranslatableMessage("event.detectorVo.analogIncrease", prettyLimit);
            else if(checkDecrease)
                return new TranslatableMessage("event.detectorVo.analogDecrease", prettyLimit);
            else
                throw new ShouldNeverHappenException("Illegal state for analog change detector" + xid);
		}
	}
	
	@Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
	    super.jsonWrite(writer);
	    writer.writeEntry("valueEventType", UPDATE_EVENT_TYPE_CODES.getCode(updateEvent));
	}

	@Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
	    super.jsonRead(reader, jsonObject);
	    String valueEventTypeCode = jsonObject.getString("valueEventType");
	    if(valueEventTypeCode != null) {
            int candidate = UPDATE_EVENT_TYPE_CODES.getId(valueEventTypeCode);
            if(candidate == -1)
                throw new TranslatableJsonException("emport.error.invalid", "publishType", 
                        valueEventTypeCode, UPDATE_EVENT_TYPE_CODES.getCodeList());
            updateEvent = candidate;
	    }
	}

}
