/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.dwr;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.license.DataSourceTypePointsLimit;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.vo.DataPointNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.beans.DataPointDefaulter;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.validation.StringValidation;

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
    
        if(id == Common.NEW_ID){
            vo = dao.getNewVo();
            //TODO Need to sort this out another way, this will wreck 
            // when users have mulitple tabs open in a browser
            DataSourceVO<?> ds = Common.getUser().getEditDataSource();
            vo.setXid(dao.generateUniqueXid());
            vo.setPointLocator(ds.createPointLocator());
            vo.setDataSourceId(ds.getId());
            vo.setDataSourceName(ds.getName());
            vo.setDataSourceTypeName(ds.getTypeKey());
            vo.setDataSourceXid(ds.getXid());
        }else{
            vo = dao.getFull(id);
        }
        
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
    @DwrPermission(admin = true)
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
    
}
