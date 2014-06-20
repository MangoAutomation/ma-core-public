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

import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

/**
 * @author Terry Packer
 * 
 */

@Controller
@RequestMapping("/v1/dataSources")
public class DataSourceController extends MangoRestController<DataSourceVO<?>>{

	private static Logger LOG = Logger.getLogger(DataSourceController.class);
	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DataSourceVO<?>> getAllDataSources() {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all data sources");
        List<DataSourceVO<?>> dataSources = DataSourceDao.instance.getAll();
        return dataSources;
    }
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<DataSourceVO<?>> viewOrder(@PathVariable String xid) {

        DataSourceVO<?> vo = DataSourceDao.instance.getByXid(xid);

        if (vo == null) {
            return new ResponseEntity<DataSourceVO<?>>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<DataSourceVO<?>>(vo, HttpStatus.OK);
    }
	
	
	
}
