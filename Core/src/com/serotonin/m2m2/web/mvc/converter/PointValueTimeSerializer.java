/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.converter;

import java.io.IOException;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.ClassSerializer;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;

/**
 * @author Terry Packer
 *
 */
public class PointValueTimeSerializer implements ClassSerializer<PointValueTime>{

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.ClassSerializer#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject)
	 */
	@Override
	public PointValueTime jsonRead(JsonReader reader, JsonObject jsonObject)
			throws JsonException {
		
		//TODO Need a way to deal with the annotations...
		String annotationText = jsonObject.getString("annotation");
		
		String dataTypeString = jsonObject.getString("dataType");
		if(dataTypeString == null)
			throw new TranslatableJsonException("emport.error.missing", "dataType", dataTypeString,
					DataTypes.CODES.getCodeList(DataTypes.IMAGE, DataTypes.UNKNOWN));
		
		int dataType = DataTypes.CODES.getId(dataTypeString);
		DataValue dataValue;
		
		switch(dataType){
		case DataTypes.BINARY:
			dataValue = new BinaryValue(jsonObject.getBoolean("value"));
			break;
		case DataTypes.MULTISTATE:
			dataValue = new MultistateValue(jsonObject.getInt("value"));
			break;
		case DataTypes.NUMERIC:
			dataValue = new NumericValue(jsonObject.getDouble("value"));
			break;
		case DataTypes.ALPHANUMERIC:
			dataValue = new AlphanumericValue(jsonObject.getString("value"));
			break;
		default:
			throw new TranslatableJsonException("emport.error.invalid", "dataType", dataTypeString,
					DataTypes.CODES.getCodeList(DataTypes.IMAGE, DataTypes.UNKNOWN));
		}
	
		long timestamp = jsonObject.getLong("time");
		
		if(annotationText.isEmpty()){
			return new PointValueTime(dataValue, timestamp);			
		}else{
			//TODO This will effectively kill translatability
			return new AnnotatedPointValueTime(dataValue, timestamp, new TranslatableMessage("common.default", annotationText));
		}
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.ClassSerializer#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject, java.lang.Object)
	 */
	@Override
	public PointValueTime jsonRead(JsonReader reader, JsonObject jsonObject,
			PointValueTime notUsed) throws JsonException {
		return this.jsonRead(reader, jsonObject);
	}

	/**
	 * 
	 * 
	 * Form:
	 * 
	 * { 
	 *   annotation: 'translated text' OR empty,
	 *   dataType: 'NUMERIC',
	 *   value: valueOfTypeDataType,
	 *   time: timestamp
	 * 
	 * }
	 * 
	 */
	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.ClassSerializer#jsonWrite(com.serotonin.json.ObjectWriter, java.lang.Object)
	 */
	@Override
	public void jsonWrite(ObjectWriter writer, PointValueTime pvt)
			throws IOException, JsonException {
		
		//TODO Work out how to serialize this, do we just want the message?
		// probably will need to expand the REST API for this
		if(pvt.isAnnotated())
			writer.writeEntry("annotation", ((AnnotatedPointValueTime)pvt).getAnnotation(Common.getTranslations()));
		else
			writer.writeEntry("annotation", new String());
		
		
		writer.writeEntry("dataType", DataTypes.CODES.getCode(pvt.getValue().getDataType()));
		
		switch(pvt.getValue().getDataType()){
		case DataTypes.ALPHANUMERIC:
			writer.writeEntry("value", pvt.getStringValue());
			break;
		case DataTypes.BINARY:
			writer.writeEntry("value", pvt.getBooleanValue());
			break;
		case DataTypes.MULTISTATE:
			writer.writeEntry("value", pvt.getIntegerValue());
			break;
		case DataTypes.NUMERIC:
			writer.writeEntry("value", pvt.getDoubleValue());
			break;
		case DataTypes.IMAGE:
		case DataTypes.UNKNOWN:
			new TranslatableJsonException("emport.error.invalid", "dataType", DataTypes.CODES.getCode(pvt.getValue().getDataType()),
					DataTypes.CODES.getCodeList(DataTypes.IMAGE, DataTypes.UNKNOWN));
		}
		
		writer.writeEntry("time", pvt.getTime());
	}




}
