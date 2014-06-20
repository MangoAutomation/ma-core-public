/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller.rest;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

/**
 * @author Terry Packer
 * 
 */

@Controller
@RequestMapping("/v1/pointValues")
public class PointValueController extends MangoRestController<PointValueTime>{

	private static Logger LOG = Logger.getLogger(PointValueController.class);
	private PointValueDao dao = Common.databaseProxy.newPointValueDao();
	
	
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<PointValueTime>> getDataPoints(@RequestParam(value="dataPointXid", required=false) String dataPointXid,
    		@RequestParam(value="limit", required=false, defaultValue="100") int limit){
    	//@RequestParam(value="after", required=false, defaultValue="") final LocalDate after) {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting Point Values for data point with Xid: " + dataPointXid);
       
    	DataPointVO vo = DataPointDao.instance.getByXid(dataPointXid);
    	if(vo == null)
    		return new ResponseEntity<List<PointValueTime>>(HttpStatus.NOT_FOUND);
    	
    	List<PointValueTime> pvts = dao.getLatestPointValues(vo.getId(), limit);
    	
        return new ResponseEntity<List<PointValueTime>>(pvts, HttpStatus.OK);
        
    }

    @RequestMapping(method = RequestMethod.GET, value="/{xid}/latest")
    public ResponseEntity<List<PointValueTime>> getLatestPointValues(@PathVariable String xid,
    		@RequestParam(value="limit", required=false, defaultValue="100") int limit){
    		
       //@RequestParam(value="after", required=false, defaultValue="") final LocalDate after) {
    	DataPointVO vo = DataPointDao.instance.getByXid(xid);
    	if(vo == null)
    		return new ResponseEntity<List<PointValueTime>>(HttpStatus.NOT_FOUND);
    	
    	List<PointValueTime> pvts = dao.getLatestPointValues(vo.getId(), limit);
    	
        return new ResponseEntity<List<PointValueTime>>(pvts, HttpStatus.OK);
        
    }
    
    /**
     * Save a new point value into the system
     * @param pvt
     * @param xid
     * @param builder
     * @return
     */
	@RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<PointValueTime> putPointValue(@RequestBody final PointValueTime pvt, @PathVariable String xid, UriComponentsBuilder builder) {
		
		ProcessResult response = new ProcessResult();
		
        DataPointVO existingDp = DataPointDao.instance.getByXid(xid);
        if (existingDp == null) {
            return new ResponseEntity<PointValueTime>(HttpStatus.NOT_FOUND);
        }
        
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

        }catch(Exception e){
        	LOG.error(e.getMessage());
        	response.addMessage(new TranslatableMessage("common.default", e.getMessage()));
        	
        	return this.createResponseEntity(response);
        	
        }

        
        //Put a link to the updated data in the header?
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(
                builder.path("/rest/v1/pointValues/{xid}")
                        .buildAndExpand(xid).toUri());
        
        return new ResponseEntity<PointValueTime>(pvt, headers, HttpStatus.OK);
    }

    
}
