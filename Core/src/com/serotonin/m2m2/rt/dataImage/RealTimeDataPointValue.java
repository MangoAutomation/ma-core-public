/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import java.io.IOException;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.util.UnitUtil;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * @author Terry Packer
 * 
 * 
 * Device Name
 * Point Name
 * Point Value
 * Units
 * Rendered Value
 * Current time stamp
 * Status (if it's reliable or not)
 * Point Hierarchy path
 * XID
 *
 *
 */
public class RealTimeDataPointValue implements JsonSerializable{
	//private static final Log LOG = LogFactory.getLog(RealTimeDataPointValue.class);
	public static String[] headers = {"deviceName","pointName","pointValue", "unit","renderedValue","timestamp","pointType","status","path","xid"};

	private DataPointRT rt;
	private String path;
	
	
	public RealTimeDataPointValue(DataPointRT rt, List<String> paths){
		this.rt = rt;
		
		//Set the path to the point
		path = "/";
		for(String part : paths){
			path = path + part + "/";
		}
		
	}

	public String getDeviceName(){
		return this.rt.getVO().getDeviceName();
	}
	public String getPointName(){
		return this.rt.getVO().getName();
	}
	/**
	 * Get the rendered value
	 * @return
	 */
	public String getRenderedPointValue(){
		PointValueTime myValue = this.getRealTimeValue();
		if(myValue != null)
			return this.rt.getVO().getTextRenderer().getText(myValue, TextRenderer.HINT_RAW);
		else
			return "";
	}
	
	/**
	 * Return the object value
	 * @return
	 */
	public Object getPointValue(){
		PointValueTime myValue = this.getRealTimeValue();
		if(myValue != null)
			return myValue.getValue().getObjectValue();
		else
			return null;
	}
	
	
	public String getPointType(){
		return this.rt.getVO().getPointLocator().getDataTypeMessage().translate(Common.getTranslations());
	}
	
	public String getUnit(){
		String unit = "";
		//Leave Units Blank
        if (this.rt.getVO().getPointLocator().getDataTypeId() == DataTypes.BINARY)
            unit = ""; //"boolean";
        else if (this.rt.getVO().getPointLocator().getDataTypeId() == DataTypes.ALPHANUMERIC)
            unit = ""; //"string";
        else if (this.rt.getVO().getPointLocator().getDataTypeId() == DataTypes.MULTISTATE)
        	unit = ""; //"multistate";
        else
            unit = UnitUtil.formatLocal(this.rt.getVO().getUnit());
        return unit;
	}

	public String getRenderedValue(){
		return this.rt.getVO().getTextRenderer().getText(this.getRealTimeValue(), TextRenderer.HINT_FULL);
	}
	
	public long getTimestamp(){
		PointValueTime myValue = this.getRealTimeValue();
		if(myValue != null)
			return myValue.getTime();
		else
			return -1; //No Value exists
	}
	public String getStatus(){
        Object unreliable = rt.getAttribute(DataSourceRT.ATTR_UNRELIABLE_KEY);
        if ((unreliable instanceof Boolean) && ((Boolean) unreliable))
            return Common.translate("common.valueUnreliable");
        else
           return "ok";
	}
	public String getPath(){
		return path;
	}
	public String getXid(){
		return this.rt.getVO().getXid();
	}
	

	/**
	 * Probably will need to go directly to the cache for this
	 * @return
	 */
	public PointValueTime getRealTimeValue(){
		return this.rt.getPointValue();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject)
	 */
	@Override
	public void jsonRead(JsonReader arg0, JsonObject arg1) throws JsonException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonWrite(com.serotonin.json.ObjectWriter)
	 */
	@Override
	public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
		writer.writeEntry("deviceName", this.getDeviceName());
		writer.writeEntry("name", this.getPointName());
		writer.writeEntry("renderedValue", this.getRenderedPointValue());
		writer.writeEntry("value", this.getPointValue());
		writer.writeEntry("unit", this.getUnit());
		writer.writeEntry("timestamp", this.getTimestamp());
		writer.writeEntry("status", this.getStatus());
		writer.writeEntry("path", this.getPath());
		writer.writeEntry("xid", this.getXid());
	}
	
	
	
}
