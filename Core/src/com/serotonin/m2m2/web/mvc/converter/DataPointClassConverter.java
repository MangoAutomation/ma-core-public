/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.converter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.convert.JsonPropertyConverter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.SerializableProperty;
import com.serotonin.json.util.TypeUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.view.text.PlainRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class DataPointClassConverter extends JsonPropertyConverter{
	private static Logger LOG = Logger.getLogger(DataPointClassConverter.class);

	private boolean jsonSerializable;
	private List<SerializableProperty> properties;
	
	/**
	 * @param jsonSerializable
	 * @param properties
	 */
	public DataPointClassConverter(boolean jsonSerializable,
			List<SerializableProperty> properties) {
		super(jsonSerializable, properties);
		this.jsonSerializable = jsonSerializable;
		this.properties = properties;
	}

	
	
	
	@Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        
		JsonObject jsonObject = (JsonObject) jsonValue;
		
		//First Setup for the full read
		String xid = jsonObject.getString("xid");

		if (StringUtils.isBlank(xid))
			xid = DataPointDao.instance.generateUniqueXid();

		DataSourceVO<?> dsvo;
		DataPointVO vo = DataPointDao.instance.getDataPoint(xid);
		if (vo == null) {
			// Locate the data source for the point.
			String dsxid = jsonObject.getString("dataSourceXid");
			dsvo = DataSourceDao.instance.getDataSource(dsxid);
			if (dsvo == null){
				String msg = translate(
						"emport.dataPoint.badReference", xid);
				LOG.error(msg);
				throw new JsonException(msg);
			}
			else {
				vo = new DataPointVO();
				vo.setXid(xid);
				vo.setDataSourceId(dsvo.getId());
				vo.setDataSourceXid(dsxid);
				vo.setPointLocator(dsvo.createPointLocator());
				vo.setEventDetectors(new ArrayList<PointEventDetectorVO>(0));
				vo.setTextRenderer(new PlainRenderer());
				return vo;
			}
		}else{
			jsonRead(reader, jsonValue, vo, type);
			return vo;
		}
    }
	
	
	@Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) throws JsonException {
		JsonObject jsonObject = (JsonObject) jsonValue;
		
	    if (jsonSerializable)
	        ((JsonSerializable) obj).jsonRead(reader, jsonObject);
	
	    if (properties != null) {
	        for (SerializableProperty prop : properties) {
	            // Check whether the property should be included
	            if (!prop.include(reader.getIncludeHint()))
	                continue;
	
	            Method writeMethod = prop.getWriteMethod();
	            if (writeMethod == null)
	                continue;
	
	            String name = prop.getNameToUse();
	
	            JsonValue propJsonValue = jsonObject.get(name);
	            if (propJsonValue == null)
	                continue;
	
	            Type propType = writeMethod.getGenericParameterTypes()[0];
	            propType = TypeUtils.resolveTypeVariable(type, propType);
	            Class<?> propClass = TypeUtils.getRawClass(propType);
	
	            try {
	                Object propValue = reader.read(propType, propJsonValue);
	
	                if (propClass.isPrimitive() && propValue == null) {
	                    if (propClass == Boolean.TYPE)
	                        propValue = false;
	                    else
	                        propValue = 0;
	                }
	
	                prop.getWriteMethod().invoke(obj, propValue);
	            }
	            catch (Exception e) {
	            	String msg = "JsonException writing property '" + prop.getName() + "' of class "
	                        + propClass.getName();
	            	LOG.error(msg);
	                throw new JsonException(msg, e);
	            }
	        }
	    }
	}
	
	
	/**
	 * Translate a message
	 * 
	 * @param key
	 * @param args
	 * @return
	 */
	private String translate(String key, Object... args) {
		return new TranslatableMessage(key, args).translate(Common
				.getTranslations());
	}
	
	

	
}
