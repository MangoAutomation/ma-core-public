/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.PointValueTimeModel;
import com.wordnik.swagger.annotations.Api;

/**
 * @author Terry Packer
 * 
 */

@Api(value="Point Values", description="Operations on Point Values")
@RestController
@RequestMapping("/v1/pointValues")
public class PointValueRestController extends MangoRestController<PointValueTimeModel>{

	private static Logger LOG = Logger.getLogger(PointValueRestController.class);
	private PointValueDao dao = Common.databaseProxy.newPointValueDao();

	
	/**
	 * Get the latest point values for a point
	 * @param xid
	 * @param limit
	 * @return
	 */
    @RequestMapping(method = RequestMethod.GET, value="/{xid}/latest")
    public ResponseEntity<List<PointValueTimeModel>> getLatestPointValues(HttpServletRequest request, @PathVariable String xid,
    		@RequestParam(value="limit", required=false, defaultValue="100") int limit){
        
    	RestProcessResult<List<PointValueTimeModel>> result = new RestProcessResult<List<PointValueTimeModel>>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    	
	    	DataPointVO vo = DataPointDao.instance.getByXid(xid);
	    	if(vo == null){
	    		result.addRestMessage(getDoesNotExistMessage());
	    		return result.createResponseEntity();
	    	}

	    	try{
	    		if(Permissions.hasDataPointReadPermission(user, vo)){
	    			List<PointValueTime> pvts = dao.getLatestPointValues(vo.getId(), limit);
	    			List<PointValueTimeModel> models = new ArrayList<PointValueTimeModel>(pvts.size());
	    			for(PointValueTime pvt : pvts){
	    				models.add(new PointValueTimeModel(pvt));
	    			}
	    			return result.createResponseEntity(models);
	    		}else{
	    	 		result.addRestMessage(getUnauthorizedMessage());
		    		return result.createResponseEntity();
		    		}
	    	}catch(PermissionException e){
	    		LOG.error(e.getMessage());
	    		result.addRestMessage(getUnauthorizedMessage());
	    		return result.createResponseEntity();
	    	}
    	}else{
    		return result.createResponseEntity();
    	}
    	
    	
        
    }
    
    /**
     * Save a new point value into the system
     * @param pvt
     * @param xid
     * @param builder
     * @return
     */
	@RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<PointValueTimeModel> putPointValue(HttpServletRequest request, @RequestBody final PointValueTime pvt, @PathVariable String xid, UriComponentsBuilder builder) {
		
		RestProcessResult<PointValueTimeModel> result = new RestProcessResult<PointValueTimeModel>(HttpStatus.OK);
		
		User user = this.checkUser(request, result);
		if(result.isOk()){
		
	        DataPointVO existingDp = DataPointDao.instance.getByXid(xid);
	        if (existingDp == null) {
	        	result.addRestMessage(getDoesNotExistMessage());
	        	return result.createResponseEntity();
	    	}
	        
	    	try{
	    		if(Permissions.hasDataPointReadPermission(user, existingDp)){
	    			
	    			//TODO Do we want to use a provided time or let the RTM Decide the time?
	    	        final int dataSourceId = existingDp.getDataSourceId();
	    	        SetPointSource source = null;
	    	        if(pvt instanceof AnnotatedPointValueTime){
	    	        	source = new SetPointSource(){
	
	    					@Override
	    					public String getSetPointSourceType() {
	    						return "REST";
	    					}
	
	    					@Override
	    					public int getSetPointSourceId() {
	    						return dataSourceId;
	    					}
	
	    					@Override
	    					public TranslatableMessage getSetPointSourceMessage() {
	    						return ((AnnotatedPointValueTime)pvt).getSourceMessage();
	    					}
	
	    					@Override
	    					public void raiseRecursionFailureEvent() {
	    						//TODO Flesh this out
	    						LOG.error("Recursive failure while setting point via REST");
	    					}
	    	        		
	    	        	};
	    	        }
	    	        try{
	    	        	Common.runtimeManager.setDataPointValue(existingDp.getId(), pvt, source);

	    	        	URI location = builder.path("/rest/v1/pointValue/{xid}/{time}").buildAndExpand(xid, pvt.getTime()).toUri();
	    		    	result.addRestMessage(getResourceCreatedMessage(location));
	    		        return result.createResponseEntity(new PointValueTimeModel(pvt));
	
	    	        }catch(Exception e){
	    	        	LOG.error(e.getMessage());
	    	        	result.addRestMessage(getInternalServerErrorMessage(e.getMessage()));
	    	        	return result.createResponseEntity();
	    	        	
	    	        }
	    			
	    			
	    		}else{
		    		result.addRestMessage(getUnauthorizedMessage());
		    		return result.createResponseEntity();
	    		}
	    	}catch(PermissionException e){
	    		LOG.error(e.getMessage());
	    		result.addRestMessage(getUnauthorizedMessage());
	    		return result.createResponseEntity();
	    	}
		}else{
			return result.createResponseEntity();
		}
    }
    
}
