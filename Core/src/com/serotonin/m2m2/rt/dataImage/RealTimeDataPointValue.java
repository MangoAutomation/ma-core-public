/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.dataImage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataPointTagsDao;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.util.UnitUtil;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointSummary;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.RoleVO;

/**
 * 
 * @author Terry Packer
 */
public class RealTimeDataPointValue implements JsonSerializable, DataPointListener {
	
	private final String DISABLED = "DISABLED";
	private final String OK = "OK";
	private final String UNRELIABLE = "UNRELIABLE";
	
	public static String[] headers = {"deviceName","pointName","pointValue", "unit","renderedValue","timestamp","pointType","status","path","xid"};

	DataPointVO vo;
	private DataPointRT rt;
	private String path;

	
	RealTimeDataPointValue(DataPointSummary summary, List<String> paths){
	    this.vo = DataPointDao.getInstance().get(summary.getId(), true);
        this.rt = Common.runtimeManager.getDataPoint(summary.getId());
		Common.runtimeManager.addDataPointListener(summary.getId(), this);
		
		//Set the path to the point
        this.path = "/";
		for(String part : paths){
			this.path = this.path + part + "/";
		}
	}

	public String getDeviceName(){
		return vo.getDeviceName();
	}
	
	public String getPointName(){
		return vo.getName();
	}
	
	/**
	 * Get the rendered value
	 * @return
	 */
	public String getRenderedValue(){
		PointValueTime myValue = this.getRealTimeValue();
		if(myValue != null)
			return this.rt.getVO().getTextRenderer().getText(myValue, TextRenderer.HINT_FULL);
		else
			return "";
	}

	/**
	 * Return the object value
	 * @return
	 */
	public Object getValue(){
		PointValueTime myValue = this.getRealTimeValue();
		if(myValue != null)
			return myValue.getValue().getObjectValue();
		else
			return null;
	}
	
	public String getPointType(){
		return vo.getPointLocator().getDataTypeMessage().translate(Common.getTranslations());
	}
	
	public String getUnit(){
        return UnitUtil.formatLocal(vo.getUnit());
	}
	
	public long getTimestamp(){
		PointValueTime myValue = this.getRealTimeValue();
		if(myValue != null)
			return myValue.getTime();
		else
			return -1; //No Value exists
	}
	public String getStatus(){
		if(rt == null)
			return DISABLED;
		
        Object unreliable = rt.getAttribute(DataSourceRT.ATTR_UNRELIABLE_KEY);
        if ((unreliable instanceof Boolean) && ((Boolean) unreliable))
            return UNRELIABLE;
        else
           return OK;
	}
	public String getPath(){
		return path;
	}
	
	public String getXid(){
		return vo.getXid();
	}
	

	/**
	 * Probably will need to go directly to the cache for this
	 * @return
	 */
	public PointValueTime getRealTimeValue(){
		if(this.rt == null)
			return null;
		else
			return this.rt.getPointValue();
	}

	public Set<RoleVO> getReadRoles(){
		return vo.getReadRoles();
	}
	
	public Set<RoleVO> getSetRoles(){
		return vo.getSetRoles();
	}
	
	/**
     * @return the tags
     */
    public Map<String, String> getTags() {
        if(rt != null) {
            Map<String, String> tags = rt.getVO().getTags();
            if(tags == null) {
                tags = DataPointTagsDao.getInstance().getTagsForDataPointId(vo.getId());
                rt.getVO().setTags(tags);
                tags = rt.getVO().getTags();
            }
            return tags;
        }else {
            Map<String, String> tags = DataPointTagsDao.getInstance().getTagsForDataPointId(vo.getId());
            vo.setTags(tags);
            return tags;
        }
    }
	
    /**
     * Terminate our listener
     */
    public void destroy() {
        Common.runtimeManager.removeDataPointListener(vo.getId(), this);
    }
    

	@Override
	public void jsonRead(JsonReader arg0, JsonObject arg1) throws JsonException {
		
	}

	@Override
	public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
		writer.writeEntry("deviceName", this.getDeviceName());
		writer.writeEntry("name", this.getPointName());
		writer.writeEntry("renderedValue", this.getRenderedValue());
		writer.writeEntry("value", this.getValue());
		writer.writeEntry("unit", this.getUnit());
		writer.writeEntry("timestamp", this.getTimestamp());
		writer.writeEntry("status", this.getStatus());
		writer.writeEntry("path", this.getPath());
		writer.writeEntry("xid", this.getXid());
		writer.writeEntry("readRoles", getReadRoles());
		writer.writeEntry("setRoles", getSetRoles());
		writer.writeEntry("tags", getTags());
	}

	@Override
	public void pointInitialized() {
		this.rt = Common.runtimeManager.getDataPoint(vo.getId());
		this.vo = rt.getVO();
		
	}

	@Override
	public void pointTerminated(DataPointVO vo) {
	    this.vo = vo;
		this.rt = null;
	}
	
	@Override
	public void pointUpdated(PointValueTime newValue) { }

	@Override
	public void pointChanged(PointValueTime oldValue, PointValueTime newValue) { }

	@Override
	public void pointSet(PointValueTime oldValue, PointValueTime newValue) { }

	@Override
	public void pointBackdated(PointValueTime value) { }

	@Override
	public String getListenerName() {
		return "RealTimeData: " + getXid(); 
	}

	@Override
	public void pointLogged(PointValueTime value) { }	
	
	public int getDataPointId(){
		return this.vo.getId();
	}
	
	public int getDataTypeId(){
		return this.vo.getPointLocator().getDataTypeId();
	}
	
}
