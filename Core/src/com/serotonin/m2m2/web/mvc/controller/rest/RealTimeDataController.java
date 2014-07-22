/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.RealTimeDataPointValue;
import com.serotonin.m2m2.rt.dataImage.RealTimeDataPointValueCache;
import com.serotonin.m2m2.vo.User;
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
@Api(value="Realtime Data", description="Operations on Real time data", position=2)
@Controller
@RequestMapping("/v1/realtime")
public class RealTimeDataController {

	private static Logger LOG = Logger.getLogger(RealTimeDataController.class);
	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<RealTimeDataPointValue> getAll(HttpServletRequest request) {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all real time data");
    	
    	User user = Common.getUser(request);
    	return RealTimeDataPointValueCache.instance.getAll(user);
    }
	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<RealTimeDataPointValue> get(@PathVariable String xid, HttpServletRequest request) {
		
		User user = Common.getUser(request);
		RealTimeDataPointValue rtpv = RealTimeDataPointValueCache.instance.get(xid, user);

        if (rtpv == null) {
            return new ResponseEntity<RealTimeDataPointValue>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<RealTimeDataPointValue>(rtpv, HttpStatus.OK);
    }
	
	
	
}
