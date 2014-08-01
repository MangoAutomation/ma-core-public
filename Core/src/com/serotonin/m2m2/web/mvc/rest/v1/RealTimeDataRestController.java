/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.RealTimeDataPointValue;
import com.serotonin.m2m2.rt.dataImage.RealTimeDataPointValueCache;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.RealTimeModel;
import com.wordnik.swagger.annotations.Api;

/**
 * 
 * Real time data controller returns RealTimeDataPointValues
 * 
 * 
 * 
 * @author Terry Packer
 * 
 */
@Api(value="Realtime Data", description="Operations on Real time data", position=5)
@RestController
@RequestMapping("/v1/realtime")
public class RealTimeDataRestController extends MangoRestController<RealTimeModel>{

	private static Logger LOG = Logger.getLogger(RealTimeDataRestController.class);
	
	/**
	 * Get all of the Users Real Time Data
	 * @param request
	 * @param limit
	 * @return
	 */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<RealTimeModel>> getAll(HttpServletRequest request, 
    		@RequestParam(value="limit", required=false, defaultValue="100")int limit) {
    	
    	RestProcessResult<List<RealTimeModel>> result = new RestProcessResult<List<RealTimeModel>>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	
    	if(result.isOk()){
	    	List<RealTimeDataPointValue> values = RealTimeDataPointValueCache.instance.getAll(user);
	    	List<RealTimeModel> models = new ArrayList<RealTimeModel>();
	    	int counter = 0;
	    	for(RealTimeDataPointValue value : values){
	    		models.add(new RealTimeModel(value));
	    		counter++;
	    		if(counter == limit)
	    			break;
	    	}
	    	return result.createResponseEntity(models);
    	}
    	
    	return result.createResponseEntity();
    	
    }
	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<RealTimeModel> get(@PathVariable String xid, HttpServletRequest request) {
		
		RestProcessResult<RealTimeModel> result = new RestProcessResult<RealTimeModel>(HttpStatus.OK);
		User user = checkUser(request, result); //Check the user
		
		//If no messages then go for it
		if(result.isOk()){
			
			RealTimeDataPointValue rtpv = RealTimeDataPointValueCache.instance.get(xid, user);
	
	        if (rtpv == null) {
	        	result.addRestMessage(HttpStatus.NOT_FOUND, new TranslatableMessage("common.default", "Point doesn't exist or is not enabled."));
	            return result.createResponseEntity();
	        }
	        RealTimeModel model = new RealTimeModel(rtpv);
	        return result.createResponseEntity(model);
	        
		}else{
			return result.createResponseEntity();
		}
    }
	
	
}
