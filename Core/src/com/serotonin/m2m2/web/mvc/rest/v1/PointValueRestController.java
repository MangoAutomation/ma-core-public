/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.net.URI;
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

/**
 * @author Terry Packer
 * 
 */
//
//@Controller
//@RequestMapping("/v1/pointValues")
public class PointValueRestController extends MangoRestController<PointValueTime>{

	private static Logger LOG = Logger.getLogger(PointValueRestController.class);
	private PointValueDao dao = Common.databaseProxy.newPointValueDao();

	
	/**
	 * Get the latest point values for a point
	 * @param xid
	 * @param limit
	 * @return
	 */
    @RequestMapping(method = RequestMethod.GET, value="/{xid}/latest")
    public ResponseEntity<List<PointValueTime>> getLatestPointValues(HttpServletRequest request, @PathVariable String xid,
    		@RequestParam(value="limit", required=false, defaultValue="100") int limit){
        
    	ProcessResult response = new ProcessResult();
    	DataPointVO vo = DataPointDao.instance.getByXid(xid);
    	if(vo == null){
    		//TODO Add to messages or extract to superclass
    		response.addMessage(new TranslatableMessage("common.default", "Point Does not exist"));
    		return this.createResponseEntityList(response, HttpStatus.NOT_FOUND);
    	}
    		
    	//Check permissions
    	User user = Common.getUser(request);
    	try{
    		if(Permissions.hasDataPointReadPermission(user, vo)){
    			List<PointValueTime> pvts = dao.getLatestPointValues(vo.getId(), limit);
    	        return new ResponseEntity<List<PointValueTime>>(pvts, HttpStatus.OK);
    		}else{
    			//TODO add to translations
    			response.addMessage(new TranslatableMessage("common.default", "Do not have permissions to access point"));
        		return this.createResponseEntityList(response, HttpStatus.FORBIDDEN);
    		}
    	}catch(PermissionException e){
    		LOG.error(e.getMessage());
        	response.addMessage(new TranslatableMessage("common.default", e.getMessage()));
    		return this.createResponseEntityList(response);
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
    public ResponseEntity<PointValueTime> putPointValue(HttpServletRequest request, @RequestBody final PointValueTime pvt, @PathVariable String xid, UriComponentsBuilder builder) {
		
		ProcessResult response = new ProcessResult();
		
        DataPointVO existingDp = DataPointDao.instance.getByXid(xid);
        if (existingDp == null) {
    		//TODO Add to messages or extract to superclass
    		response.addMessage(new TranslatableMessage("common.default", "Point Does not exist"));
    		return this.createResponseEntity(response, HttpStatus.NOT_FOUND);
    	}
        
        User user = Common.getUser(request);
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
    	            
    	        	URI location = builder.path("/rest/v1/pointValues/{xid}")
                            .buildAndExpand(xid).toUri();
    	            ResponseEntity<PointValueTime> entity =  this.createResponseEntity(location, response, pvt, HttpStatus.CREATED);
    	            return entity;

    	        }catch(Exception e){
    	        	LOG.error(e.getMessage());
    	        	response.addMessage(new TranslatableMessage("common.default", e.getMessage()));
    	        	
    	        	return this.createResponseEntity(response);
    	        	
    	        }
    			
    			
    		}else{
    			//TODO add to translations
    			response.addMessage(new TranslatableMessage("common.default", "Do not have permissions to access point"));
        		return this.createResponseEntity(response, HttpStatus.FORBIDDEN);
    		}
    	}catch(PermissionException e){
    		LOG.error(e.getMessage());
        	response.addMessage(new TranslatableMessage("common.default", e.getMessage()));
    		return this.createResponseEntity(response);
    	}
    }
    
}
