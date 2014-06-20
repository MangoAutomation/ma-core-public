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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

/**
 * @author Terry Packer
 * 
 */

@Controller
@RequestMapping("/v1/dataPoints")
public class DataPointController extends MangoRestController<DataPointVO>{

	private static Logger LOG = Logger.getLogger(DataPointController.class);
	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DataPointVO> getAllDataPoints() {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all data points");
        List<DataPointVO> dataPoints = DataPointDao.instance.getAll();
        return dataPoints;
    }
	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<DataPointVO> getDataPoint(@PathVariable String xid) {

        DataPointVO vo = DataPointDao.instance.getByXid(xid);

        if (vo == null) {
            return new ResponseEntity<DataPointVO>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<DataPointVO>(vo, HttpStatus.OK);
    }
	
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<DataPointVO> updateDataPoint(@RequestBody DataPointVO vo, @PathVariable String xid, UriComponentsBuilder builder) {

        DataPointVO existingDp = DataPointDao.instance.getByXid(xid);
        if (existingDp == null) {
            return new ResponseEntity<DataPointVO>(HttpStatus.NOT_FOUND);
        }
        
        //We do not read the XID or ID via JSON
        //TODO One reason to use a custom JSON converter
        vo.setXid(xid);
        vo.setId(existingDp.getId());
        
        ProcessResult response = new ProcessResult();
        vo.validate(response);
        
        if(response.getHasMessages()){
        	 return createResponseEntity(response); 
        }else{

        	//We will always override the DS Info with the one from the XID Lookup
            DataSourceVO<?> dsvo = DataSourceDao.instance.getDataSource(existingDp.getDataSourceXid());
            
            //TODO this implies that we may need to have a different JSON Converter for data points
            //Need to set DataSourceId among other things
            vo.setDataSourceId(existingDp.getDataSourceId());
            
            
            if (dsvo == null){
            	response.addGenericMessage("emport.dataPoint.badReference", xid);
            	return createResponseEntity(response);
            }else {
                //Compare this point to the existing point in DB to ensure
                // that we aren't moving a point to a different type of Data Source
                DataPointDao dpDao = new DataPointDao();
                DataPointVO oldPoint = dpDao.getDataPoint(vo.getId());
                
                //Does the old point have a different data source?
                if(oldPoint != null&&(oldPoint.getDataSourceId() != dsvo.getId())){
                    vo.setDataSourceId(dsvo.getId());
                    vo.setDataSourceName(dsvo.getName());
                }
            }

            Common.runtimeManager.saveDataPoint(vo);
        }
        
        //Put a link to the updated data in the header?
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(
                builder.path("/rest/v1/dataPoints/{xid}")
                        .buildAndExpand(xid).toUri());
        
        return new ResponseEntity<DataPointVO>(vo, headers, HttpStatus.OK);
    }
	



	@RequestMapping(method = RequestMethod.DELETE, value = "/{xid}")
    public ResponseEntity<DataPointVO> delete(@PathVariable String xid) {
		
		//TODO Fix up to use delete by XID?
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		if (vo == null) {
            return new ResponseEntity<DataPointVO>(HttpStatus.NOT_FOUND);
        }
		
		DataPointDao.instance.delete(vo.getId());
        
		return new ResponseEntity<DataPointVO>(vo, HttpStatus.OK);
    }
	
	
}
