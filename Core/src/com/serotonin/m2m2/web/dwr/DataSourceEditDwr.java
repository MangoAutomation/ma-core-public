/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.license.DataSourceTypePointsLimit;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.DataPointNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.BasicDataSourceVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.web.dwr.beans.DataPointDefaulter;
import com.serotonin.m2m2.web.dwr.beans.EventInstanceBean;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.m2m2.web.taglib.Functions;
import com.serotonin.validation.StringValidation;

/**
 * @author Matthew Lohbihler
 */
public class DataSourceEditDwr extends DataSourceListDwr {
    //
    //
    // Common methods
    //
    @DwrPermission(user = true)
    public ProcessResult editInit() {
        ProcessResult response = new ProcessResult();
        response.addData("points", getPoints());
        response.addData("alarms", getAlarms());
        return response;
    }

    protected void setBasicProps(DataSourceVO<?> ds, BasicDataSourceVO basic) {
        ds.setName(basic.getName());
        ds.setXid(basic.getXid());
        ds.setPurgeOverride(basic.isPurgeOverride());
        ds.setPurgeType(basic.getPurgeType());
        ds.setPurgePeriod(basic.getPurgePeriod());
        ds.setEnabled(basic.isEnabled());
    }

    protected ProcessResult tryDataSourceSave(DataSourceVO<?> ds) {
        ProcessResult response = new ProcessResult();

        ds.validate(response);

        if (!response.getHasMessages()) {
            Common.runtimeManager.saveDataSource(ds);
            response.addData("id", ds.getId());
            response.addData("vo",ds); //For new table method
        }

        return response;
    }

    @DwrPermission(user = true)
    public void cancelTestingUtility() {
        Common.getUser().cancelTestingUtility();
    }

    @DwrPermission(user = true)
    public List<DataPointVO> getPoints() {
        User user = Common.getUser();
        if (user == null)
            return null;

        DataSourceVO<?> ds = user.getEditDataSource();
        if (ds.getId() == Common.NEW_ID)
            return null;

        List<DataPointVO> points = new DataPointDao()
                .getDataPoints(ds.getId(), DataPointNameComparator.instance, false);
        return points;
    }

    @DwrPermission(user = true)
    public DataPointVO getPoint(int pointId) {
        return getPoint(pointId, null);
    }

    protected DataPointVO getPoint(int pointId, DataPointDefaulter defaulter) {
        //Added to allow saving point settings from data point edit view
        DataPointVO dp = Common.getUser().getEditPoint();
        DataSourceVO<?> ds = Common.getUser().getEditDataSource();
        
        //Another Kludge to allow modules to get new-ish data points via this method...
        //TODO This affects the data source tools so be careful with changes.
        if((dp==null)&&(pointId == Common.NEW_ID)){
        	dp = new DataPointVO();
        	dp.setId(pointId);       	
        	dp.setXid(DataPointDao.instance.generateUniqueXid());
            dp.setDataSourceId(ds.getId());
            dp.setDataSourceTypeName(ds.getDefinition().getDataSourceTypeName());
            dp.setDeviceName(ds.getName());
            dp.setPointLocator(ds.createPointLocator());
            dp.setEventDetectors(new ArrayList<PointEventDetectorVO>(0));
            dp.defaultTextRenderer();
        }else if((dp!=null)&&(pointId != dp.getId())){
        	if(pointId == -1){
        		dp = new DataPointVO();
            	dp.setId(pointId);       	
            	dp.setXid(DataPointDao.instance.generateUniqueXid());
                dp.setDataSourceId(ds.getId());
                dp.setDataSourceTypeName(ds.getDefinition().getDataSourceTypeName());
                dp.setDeviceName(ds.getName());
                dp.setPointLocator(ds.createPointLocator());
                dp.setEventDetectors(new ArrayList<PointEventDetectorVO>(0));
                dp.defaultTextRenderer();	
        	}else
        		dp = DataPointDao.instance.get(pointId);
        }
        
        //Use the defaulter
        if(defaulter != null){
        	if(dp.getId() == Common.NEW_ID)
        		defaulter.setDefaultValues(dp);
        	else
        		defaulter.updateDefaultValues(dp);
        }
        
        return dp;
    }

    protected ProcessResult validatePoint(int id, String xid, String name, PointLocatorVO locator,
            DataPointDefaulter defaulter) {
        return validatePoint(id, xid, name, locator, defaulter, true);
    }

    protected ProcessResult validatePoint(int id, String xid, String name, PointLocatorVO locator,
            DataPointDefaulter defaulter, boolean includePointList) {
        ProcessResult response = new ProcessResult();

        //This saving of the point into the User is a bad idea, need to rework to
        // pass the point back and forth to page.  
        DataPointVO dp = getPoint(id, defaulter);
        dp.setXid(xid);
        dp.setName(name);
        dp.setPointLocator(locator);

        
        //If we are a new point then only validate the basics
        if (id == Common.NEW_ID){
            // Limit enforcement.
            DataSourceTypePointsLimit.checkLimit(dp.getDataSourceTypeName(), response);
            
            if (StringUtils.isBlank(xid))
                response.addContextualMessage("xid", "validate.required");
            else if (StringValidation.isLengthGreaterThan(xid, 50))
                response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 50));
            else if (!new DataPointDao().isXidUnique(xid, id))
                response.addContextualMessage("xid", "validate.xidUsed");

            if (StringUtils.isBlank(name))
                response.addContextualMessage("name", "validate.required");
            
            //Should really be done elsewhere
            dp.setEventDetectors(new ArrayList<PointEventDetectorVO>());
            
        }else{
	        //New validation on save for all settings on existing points
        	
	        dp.validate(response);
	        
	        if(dp.getChartRenderer() != null)
	        	dp.getChartRenderer().validate(response);
	        
	        if(dp.getTextRenderer() != null)
	        	dp.getTextRenderer().validate(response);
        }
        //Validate Locator
        locator.validate(response, dp);

        if (!response.getHasMessages()) {
        	

        	
            Common.runtimeManager.saveDataPoint(dp);
        	
            //If we have the need to copy permissions then do it now
            // Dirty kludge but whatever for now this all needs reworking.
            if(dp.getCopyPermissionsFrom() > 0){
        		DataPointDao.instance.copyPermissions(dp.getCopyPermissionsFrom(), dp.getId());
        	}
        	
            if (defaulter != null)
                defaulter.postSave(dp);
            response.addData("id", dp.getId());
            response.addData("vo",dp);
            if (includePointList)
                response.addData("points", getPoints());
            //Set the User Point
            Common.getUser().setEditPoint(dp);
        }

        return response;
    }

    @DwrPermission(user = true)
    public List<DataPointVO> deletePoint(int id) {
        DataPointVO dp = getPoint(id, null);
        if (dp != null)
            Common.runtimeManager.deleteDataPoint(dp);

        return getPoints();
    }

    @DwrPermission(user = true)
    public Map<String, Object> toggleEditDataSource() {
        DataSourceVO<?> ds = Common.getUser().getEditDataSource();
        Map<String, Object> result = super.toggleDataSource(ds.getId());
        ds.setEnabled((Boolean) result.get("enabled"));
        return result;
    }

    @DwrPermission(user = true)
    public ProcessResult togglePoint(int dataPointId) {
        ProcessResult response = super.toggleDataPoint(dataPointId);
        response.addData("points", getPoints());
        return response;
    }

    @DwrPermission(user = true)
    public List<EventInstanceBean> getAlarms() {
        DataSourceVO<?> ds = Common.getUser().getEditDataSource();
        List<EventInstance> events = new EventDao().getPendingEventsForDataSource(ds.getId(), Common.getUser().getId());
        List<EventInstanceBean> beans = new ArrayList<EventInstanceBean>();
        if (events != null) {
            for (EventInstance event : events)
                beans.add(new EventInstanceBean(event.isActive(), event.getAlarmLevel(), Functions.getTime(event
                        .getActiveTimestamp()), translate(event.getMessage())));
        }
        return beans;
    }

    @DwrPermission(user = true)
    public void updateEventAlarmLevel(int eventId, int alarmLevel) {
        DataSourceVO<?> ds = Common.getUser().getEditDataSource();
        ds.setAlarmLevel(eventId, alarmLevel);
    }

    @DwrPermission(user = true)
    public String exportDataSource() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        List<DataSourceVO<?>> dss = new ArrayList<DataSourceVO<?>>();
        dss.add(Common.getUser().getEditDataSource());
        data.put(EmportDwr.DATA_SOURCES, dss);
        return EmportDwr.export(data);
    }

    @DwrPermission(user = true)
    public String exportDataPoint(int dataPointId) {
        DataSourceVO<?> ds = Common.getUser().getEditDataSource();
        DataPointVO dp = new DataPointDao().getDataPoint(dataPointId);
        if (dp == null)
            return null;
        if (dp.getDataSourceId() != ds.getId())
            throw new PermissionException("Wrong data source", Common.getUser());

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        List<DataPointVO> dss = new ArrayList<DataPointVO>();
        dss.add(dp);
        data.put(EmportDwr.DATA_POINTS, dss);
        return EmportDwr.export(data);
    }

    @DwrPermission(user = true)
    public final ProcessResult getGeneralStatusMessages() {
        ProcessResult result = new ProcessResult();

        DataSourceVO<?> vo = Common.getUser().getEditDataSource();
        DataSourceRT rt = Common.runtimeManager.getRunningDataSource(vo.getId());

        List<TranslatableMessage> messages = new ArrayList<TranslatableMessage>();
        result.addData("messages", messages);
        if (rt == null)
            messages.add(new TranslatableMessage("dsEdit.notEnabled"));
        else {
            rt.addStatusMessages(messages);
            if (messages.isEmpty())
                messages.add(new TranslatableMessage("dsEdit.noStatus"));
        }

        return result;
    }
}
