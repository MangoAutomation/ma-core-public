/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataSource.PurgeSettings;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;

/**
 * @author Terry Packer
 *
 */
@JsonPropertyOrder({"xid", "name", "enabled"})
public abstract class AbstractDataSourceModel<T extends DataSourceVO<?>> extends AbstractActionVoModel<DataSourceVO<?>>{
	
	protected T data;
	
	/**
	 * @param data
	 */
	public AbstractDataSourceModel(T data) {
		super(data);
		this.data = data;
	}
	
	@JsonGetter(value="alarmLevels")
	public Map<String,String> getAlarmLevels(){
		ExportCodes eventCodes = this.data.getEventCodes();
        Map<String, String> alarmCodeLevels = new HashMap<>();

        if (eventCodes != null && eventCodes.size() > 0) {

            for (int i = 0; i < eventCodes.size(); i++) {
                int eventId = eventCodes.getId(i);
                int level = this.data.getAlarmLevel(eventId, AlarmLevels.URGENT);
                alarmCodeLevels.put(eventCodes.getCode(eventId), AlarmLevels.CODES.getCode(level));
            }
        }
        return alarmCodeLevels;
	}
	@JsonSetter(value="alarmLevels")
	public void setAlarmLevels(Map<String,String> alarmCodeLevels) throws TranslatableJsonException{
		if (alarmCodeLevels != null) {
            ExportCodes eventCodes = this.data.getEventCodes();
            if (eventCodes != null && eventCodes.size() > 0) {
                for (String code : alarmCodeLevels.keySet()) {
                    int eventId = eventCodes.getId(code);
                    if (!eventCodes.isValidId(eventId))
                        throw new TranslatableJsonException("emport.error.eventCode", code, eventCodes.getCodeList());

                    String text = alarmCodeLevels.get(code);
                    int level = AlarmLevels.CODES.getId(text);
                    if (!AlarmLevels.CODES.isValidId(level))
                        throw new TranslatableJsonException("emport.error.alarmLevel", text, code,
                                AlarmLevels.CODES.getCodeList());

                    this.data.setAlarmLevel(eventId, level);
                }
            }
        }
	}
	
	@JsonGetter(value="purgeSettings")
	public PurgeSettings getPurgeSettings(){
		return new PurgeSettings(this.data.isPurgeOverride(), this.data.getPurgePeriod(), this.data.getPurgeType());
	}
	
	@JsonSetter("purgeSettings")
	public void setPurgeSettings(PurgeSettings settings){
		this.data.setPurgeOverride(settings.isOverride());
		this.data.setPurgePeriod(settings.getFrequency().getPeriods());
		this.data.setPurgeType(TimePeriodType.convertFrom(settings.getFrequency().getType()));
	}
	
	@JsonGetter("editPermission")
	public String getEditPermission(){
		return this.data.getEditPermission();
	}
	@JsonSetter("editPermission")
	public void setEditPermission(String editPermission){
		this.data.setEditPermission(editPermission);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel#validate(com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult)
	 */
	public void validate(RestProcessResult<?> result) throws RestValidationFailedException {
		ProcessResult validation = new ProcessResult();
		this.data.validate(validation);
		
		if(validation.getHasMessages()){
			result.addValidationMessages(validation);
			throw new RestValidationFailedException(this, result);
		}
	}
	
	@JsonIgnore
	public T getData(){
		return this.data;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel#getModelType()
	 */
	@Override
	public String getModelType() {
		return this.data.getDefinition().getDataSourceTypeName();
	}
	
	@JsonIgnore
	public void setDefinition(DataSourceDefinition dataSourceDefinition) {
		this.data.setDefinition(dataSourceDefinition);
	}
}
