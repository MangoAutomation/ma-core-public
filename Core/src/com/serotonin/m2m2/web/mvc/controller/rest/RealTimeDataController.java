/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller.rest;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * 
 * TODO Define a real time data point value to return...
 * 
 * 
 * @author Terry Packer
 * 
 */

@Controller
@RequestMapping("/v1/realtime")
public class RealTimeDataController {

	private static Logger LOG = Logger.getLogger(RealTimeDataController.class);
	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DataPointVO> getAllDataPoints() {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all real time data");
        List<DataPointVO> dataPoints = DataPointDao.instance.getAll();
        return dataPoints;
    }
	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<DataPointVO> viewOrder(@PathVariable String xid) {

        DataPointVO vo = DataPointDao.instance.getByXid(xid);

        if (vo == null) {
            return new ResponseEntity<DataPointVO>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<DataPointVO>(vo, HttpStatus.OK);
    }
	
	
	
}
