/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.pointValue;

import java.util.Date;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

/**
 * @author Terry Packer
 *
 */
public class PointValueTimeModel extends AbstractRestModel<PointValueTime>{

	public PointValueTimeModel(){
		super(null);
	}
	/**
	 * @param data
	 */
	public PointValueTimeModel(PointValueTime data) {
		super(data);
	}

	@JsonGetter("value")
	public Object getValue(){
		switch(this.data.getValue().getDataType()){
			case DataTypes.ALPHANUMERIC:
			return this.data.getStringValue();
			
			case DataTypes.BINARY:
			return this.data.getBooleanValue();
			
			case DataTypes.IMAGE:
			return null; //Not supporting for now
		
			case DataTypes.MULTISTATE:
			return this.data.getIntegerValue(); //Not supporting for now
			
			case DataTypes.NUMERIC:
			return this.data.getDoubleValue();
			
			default:
				throw new ShouldNeverHappenException("Unknown Data Type: " + this.data.getValue().getDataType());
		}
	}

	@JsonGetter("time")
	public Date getDate(){
		return new Date(this.data.getTime());
	}

	@JsonGetter("annotation")
	public String getAnnotation(){
		if(this.data instanceof AnnotatedPointValueTime){
			return ((AnnotatedPointValueTime) this.data).getAnnotation(Common.getTranslations());
		}else{
			return null;
		}
	}

	@Override
	public void validate(RestProcessResult<?> result) throws RestValidationFailedException{
		ProcessResult validation = new ProcessResult();
		
		validation.addContextualMessage("Point Value", "common.default", "Validation Not Implemented");
		
		
		if(validation.getHasMessages()){
			result.addRestMessage(HttpStatus.BAD_REQUEST, new TranslatableMessage("common.default", "Validation failed"));
			result.addValidationMessages(validation);
			throw new RestValidationFailedException(this, result);
		}
	}
	
	
}
