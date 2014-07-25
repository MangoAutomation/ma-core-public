/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractDataSourceModel;
import com.wordnik.swagger.annotations.Api;

/**
 * @author Terry Packer
 * 
 */
@Api(value="Data Sources", description="Operations on Data Sources", position=2)
@RestController
@RequestMapping("/v1/dataSources")
public class DataSourceRestController extends MangoRestController<AbstractDataSourceModel<?>>{

	public DataSourceRestController(){
		LOG.info("Creating DS Controller");
	}
	private static Logger LOG = Logger.getLogger(DataSourceRestController.class);
	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<AbstractDataSourceModel<?>> getAllDataSources() {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all data sources");
        List<DataSourceVO<?>> dataSources = DataSourceDao.instance.getAll();
        List<AbstractDataSourceModel<?>> models = new ArrayList<AbstractDataSourceModel<?>>();
        for(DataSourceVO<?> ds : dataSources)
        	models.add(ds.getModel());
        return models;
    }
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<AbstractDataSourceModel<?>> getDataSource(@PathVariable String xid) {

        DataSourceVO<?> vo = DataSourceDao.instance.getByXid(xid);

        if (vo == null) {
            return new ResponseEntity<AbstractDataSourceModel<?>>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<AbstractDataSourceModel<?>>(vo.getModel(), HttpStatus.OK);
    }
	
	
	
}
