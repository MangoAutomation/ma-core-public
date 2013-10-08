/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.license.DataSourceTypePointsLimit;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueFacade;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.view.chart.BaseChartRenderer;
import com.serotonin.m2m2.view.text.BaseTextRenderer;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.beans.DataPointDefaulter;
import com.serotonin.m2m2.web.dwr.beans.RenderedPointValueTime;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.m2m2.web.taglib.Functions;

/**
 * @author Terry Packer
 *
 */
public class DataPointDwr extends AbstractDwr<DataPointVO, DataPointDao>{

	/**
	 * Default Constructor
	 */
	public DataPointDwr(){
		super(DataPointDao.instance,"dataPoints");
		LOG = LogFactory.getLog(DataPointDwr.class);
	}
	
	
    @DwrPermission(user = true)
    public ProcessResult getPoints() {
    	ProcessResult result = new ProcessResult();
    	
        User user = Common.getUser();
        if (user == null){
        	result.addData("list",new ArrayList<DataPointVO>());
            return result;
        }

        DataSourceVO<?> ds = user.getEditDataSource();
        if (ds.getId() == Common.NEW_ID){
        	result.addData("list",new ArrayList<DataPointVO>());
            return result;
        }

        List<DataPointVO> points = new DataPointDao()
                .getDataPoints(ds.getId(), DataPointNameComparator.instance, false);
        result.addData("list", points);
        
        return result;
    }

    @DwrPermission(user = true)
    public ProcessResult toggle(int dataPointId) {
        DataPointVO dataPoint = DataPointDao.instance.getFull(dataPointId);
        Permissions.ensureDataSourcePermission(Common.getUser(), dataPoint.getDataSourceId());

        dataPoint.setEnabled(!dataPoint.isEnabled());
        Common.runtimeManager.saveDataPoint(dataPoint);

        ProcessResult response = new ProcessResult();
        response.addData("id", dataPointId);
        response.addData("enabled", dataPoint.isEnabled());
        return response;

    }
	
    /* (non-Javadoc)
     * @see com.deltamation.mango.downtime.web.AbstractBasicDwr#getFull(int)
     */
    @DwrPermission(user = true)
    @Override
    public ProcessResult getFull(int id) {
        DataPointVO vo;
    	User user = Common.getUser();
        
       
 
        
        if(id == Common.NEW_ID){
            vo = dao.getNewVo();
            //TODO Need to sort this out another way, this will wreck 
            // when users have mulitple tabs open in a browser
            DataSourceVO<?> ds = user.getEditDataSource();
            vo.setXid(dao.generateUniqueXid());
            vo.setPointLocator(ds.createPointLocator());
            vo.setDataSourceId(ds.getId());
            vo.setDataSourceName(ds.getName());
            vo.setDataSourceTypeName(ds.getTypeKey());
            vo.setDataSourceXid(ds.getXid());
        }else{
            vo = dao.getFull(id);
        }

        //Should check permissions?
        //Permissions.ensureDataSourcePermission(user, vo.getDataSourceId());
        user.setEditPoint(vo);
        //TODO NEed to deal with point value defaulter
        
        ProcessResult response = new ProcessResult();
        response.addData("vo", vo);
        
        return response;
    }
    
    
    /**
     * Delete a VO
     * @param id
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult remove(int id) {
        ProcessResult response = new ProcessResult();
        try {
        	 DataPointVO dp = dao.get(id);
        	 if(dp != null)
        		 Common.runtimeManager.deleteDataPoint(dp);
        } catch(Exception e) {
            // Handle the exceptions.
            LOG.error(e);
            DataPointVO vo = dao.get(id);
            if(e instanceof DataIntegrityViolationException)
                response.addContextualMessage(vo.getName(), "table.edit.unableToDeleteDueToConstraints");
            else
                response.addContextualMessage(vo.getName(), "table.edit.unableToDelete", e.getMessage());
        }
        
        response.addData("id", id);
        return response;
    }
    
    /**
     * Save the VO AND FDAO Data
     * 
     * Conversion for the VO must be added by extending DwrConversionDefinition
     * 
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult saveFull(DataPointVO vo) { // TODO combine with save()
        ProcessResult response = new ProcessResult();
        
        if (vo.getXid() == null) {
            vo.setXid(dao.generateUniqueXid());
        }
        vo.validate(response);
        
        // Limit enforcement.
        DataSourceTypePointsLimit.checkLimit(vo.getDataSourceTypeName(), response);
        if(!response.getHasMessages()) {

            //TODO Deail with data point defaulter...
            DataPointDefaulter defaulter = null;
            try {
                Common.runtimeManager.saveDataPoint(vo);
                if (defaulter != null)
                    defaulter.postSave(vo);
     
            } catch(Exception e) {
                // Handle the exceptions.
                LOG.error(e);
                
                String context = vo.getName();
                if (context == null) {
                    context = vo.getXid();
                }
                if (context == null) {
                    context = vo.getXid();
                }
                if (context == null) {
                    context = Integer.toString(vo.getId());
                }
                
                if(e instanceof DuplicateKeyException)
                    response.addContextualMessage(context, "downtime.edit.alreadyExists");
                else
                    response.addContextualMessage(context, "downtime.edit.unableToSave", e.getMessage());
            }
        }
        response.addData("vo", vo);
        response.addData("id", vo.getId()); //Add in case it fails
        return response;
    }
    
    /**
     * Get a list of available Chart Renderers for this point
     * @param vo
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult getChartRendererOptions(int dataTypeId) { 
    	ProcessResult response = new ProcessResult();
    	List<ImplDefinition> list = BaseChartRenderer.getImplementations(dataTypeId);
    	response.addData("options",list);
    	return response;
    	
    }
    
    /**
     * Get a list of available Chart Renderers for this point
     * @param vo
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult getTextRendererOptions(int dataTypeId) { 
    	ProcessResult response = new ProcessResult();
    	List<ImplDefinition> list = BaseTextRenderer.getImplementation(dataTypeId);
    	response.addData("options",list);
    	return response;
    	
    }
    /**
     * Get a list of available Point Event Detectors for this point
     * @param vo
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult getEventDetectorOptions(int dataTypeId) { 
    	ProcessResult response = new ProcessResult();
    	List<ImplDefinition> list = PointEventDetectorVO.getImplementations(dataTypeId);
    	response.addData("options",list);
    	return response;
    	
    }
    
    
    /**
     * Store the logging properties into the 
     * current user's edit point
     * 
     * @param type
     * @param period
     * @param periodType
     * @param intervalType
     * @param tolerance
     * @param discardExtremeValues
     * @param discardHighLimit
     * @param discardLowLimit
     * @param purgeOverride
     * @param purgeType
     * @param purgePeriod
     * @param defaultCacheSize
     */
    @Deprecated //I Think this isn't being used anymore
    @DwrPermission(user = true)
    public void storeEditLoggingProperties(int type, int period, int periodType,
    		int intervalType, double tolerance, boolean discardExtremeValues, 
    		double discardHighLimit, double discardLowLimit, 
    		boolean purgeOverride, int purgeType, int purgePeriod, int defaultCacheSize) {
    	
    	DataPointVO dp = Common.getUser().getEditPoint();  
    	if(dp!=null){
    		dp.setLoggingType(type);
    		dp.setIntervalLoggingPeriod(period);
    		dp.setIntervalLoggingPeriodType(periodType);
    		dp.setIntervalLoggingType(intervalType);
    		dp.setTolerance(tolerance);
    		dp.setDiscardExtremeValues(discardExtremeValues);
    		dp.setDiscardHighLimit(discardHighLimit);
    		dp.setDiscardLowLimit(discardLowLimit);
    		dp.setPurgeOverride(purgeOverride);
    		dp.setPurgeType(purgeType);
    		dp.setPurgePeriod(purgePeriod);
    		dp.setDefaultCacheSize(defaultCacheSize);
    	}
    	
    }
 
    @DwrPermission(user = true)
    public void storeEditProperties(DataPointVO newDp){
    	DataPointVO dp = Common.getUser().getEditPoint();  
    	if(dp!=null){
    		//Do we want the details set here? (The ID Name,XID and Locator are stored via the modules)
    		dp.setDeviceName(newDp.getDeviceName());
    		
    		//General Properties
    		dp.setEngineeringUnits(newDp.getEngineeringUnits());
    		dp.setUseIntegralUnit(newDp.isUseIntegralUnit());
    		dp.setUseRenderedUnit(newDp.isUseRenderedUnit());
    		dp.setUnit(newDp.getUnit());
    		dp.setRenderedUnit(newDp.getRenderedUnit());
    		dp.setIntegralUnit(newDp.getIntegralUnit());
    		dp.setChartColour(newDp.getChartColour());
    		dp.setPlotType(newDp.getPlotType());
    		
    		//Logging Properties
    		dp.setLoggingType(newDp.getLoggingType());
    		dp.setIntervalLoggingPeriod(newDp.getIntervalLoggingPeriod());
    		dp.setIntervalLoggingPeriodType(newDp.getIntervalLoggingPeriodType());
    		dp.setIntervalLoggingType(newDp.getIntervalLoggingType());
    		dp.setTolerance(newDp.getTolerance());
    		dp.setDiscardExtremeValues(newDp.isDiscardExtremeValues());
    		dp.setDiscardHighLimit(newDp.getDiscardHighLimit());
    		dp.setDiscardLowLimit(newDp.getDiscardLowLimit());
    		dp.setPurgeOverride(newDp.isPurgeOverride());
    		dp.setPurgeType(newDp.getPurgeType());
    		dp.setPurgePeriod(newDp.getPurgePeriod());
    		dp.setDefaultCacheSize(dp.getDefaultCacheSize());
    		
    		//Chart Renderer
    		dp.setChartRenderer(newDp.getChartRenderer());
    		
    		//Text Renderer
    		dp.setTextRenderer(newDp.getTextRenderer());
    	}
    }

    /**
     * Helper to get the most recent value for a point
     * @param id
     * @return
     */
    @DwrPermission(user = true)
    public ProcessResult getMostRecentValue(int id) {
    	ProcessResult result = new ProcessResult();

    	if(Common.runtimeManager.isDataPointRunning(id)){
    		DataPointRT rt = Common.runtimeManager.getDataPoint(id);
    		//Check to see if the data source is running
    		if(Common.runtimeManager.isDataSourceRunning(rt.getDataSourceId())){
		        PointValueFacade facade = new PointValueFacade(rt.getVO().getId());
		        PointValueTime value = facade.getPointValue();
        		if(value != null){
    				RenderedPointValueTime rpvt = new RenderedPointValueTime();
    		        rpvt.setValue(Functions.getHtmlText(rt.getVO(), value));
    		        rpvt.setTime(Functions.getTime(value));
    				result.getData().put("pointValue",rpvt.getValue()); //Could return time and value?
    			}else
    				result.getData().put("pointValue", translate("event.setPoint.activePointValue"));
    		}else{
    			result.getData().put("pointValue",translate("common.pointWarning"));
    		}
    	}else{
    		result.getData().put("pointValue",translate("common.pointWarning"));
    	}

    	return result;
    }
}
