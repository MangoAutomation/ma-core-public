/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.controller.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * @author Terry Packer
 * 
 */
@Api(value="", description="Operations on Data points", position=1)
@Controller
@RequestMapping("/v1/dataPoints")
public class DataPointController extends MangoRestController<DataPointVO>{

	private static Logger LOG = Logger.getLogger(DataPointController.class);
	
	@ApiOperation(value = "", position = 5)
    @RequestMapping(method = RequestMethod.GET, value="/help")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String help(HttpServletRequest request) {

    	InputStream is = getClass().getResourceAsStream("/com/serotonin/m2m2/web/mvc/controller/rest/dataPointResource.htm");
    	
    	try {
			return IOUtils.toString(is, Common.UTF8);
		} catch (IOException e) {
			LOG.error(e);
		}
       return "";
    }
	
	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DataPointVO> getAllDataPoints(HttpServletRequest request, 
    		@RequestParam(value="limit", required=false, defaultValue="100")int limit) {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all data points");
        List<DataPointVO> dataPoints = DataPointDao.instance.getAll();
        List<DataPointVO> userDataPoints = new ArrayList<DataPointVO>();
        
        //Filter on permissions
        User user = Common.getUser(request);
        for(DataPointVO vo : dataPoints){
        	try{
        		if(Permissions.hasDataPointReadPermission(user, vo)){
        			userDataPoints.add(vo);
        			limit--;
        		}
        		//Check the limit, TODO make this work like the DOJO Query
        		if(limit <= 0)
        			break;
        	}catch(PermissionException e){
        		//Munch it
        		//TODO maybe don't throw this from check permissions?
        	}
        }
        
        return userDataPoints;
    }
	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<DataPointVO> getDataPoint(@PathVariable String xid, HttpServletRequest request) {

        DataPointVO vo = DataPointDao.instance.getByXid(xid);
        if (vo == null) {
            return new ResponseEntity<DataPointVO>(HttpStatus.NOT_FOUND);
        }
        //Check permissions
        User user = Common.getUser(request);
    	try{
    		if(Permissions.hasDataPointReadPermission(user, vo))
    			return new ResponseEntity<DataPointVO>(vo, HttpStatus.OK);
    		else
    			return new ResponseEntity<DataPointVO>(HttpStatus.FORBIDDEN);
    	}catch(PermissionException e){
    		return new ResponseEntity<DataPointVO>(HttpStatus.FORBIDDEN);
    	}
    }
	
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<DataPointVO> updateDataPoint(@RequestBody DataPointVO vo, @PathVariable String xid, 
    		UriComponentsBuilder builder, HttpServletRequest request) {

        DataPointVO existingDp = DataPointDao.instance.getByXid(xid);
        if (existingDp == null) {
            return new ResponseEntity<DataPointVO>(HttpStatus.NOT_FOUND);
        }
        
        //Check permissions
        User user = Common.getUser(request);
    	try{
    		if(!Permissions.hasDataPointReadPermission(user, vo))
    			return new ResponseEntity<DataPointVO>(HttpStatus.FORBIDDEN);
    	}catch(PermissionException e){
    		return new ResponseEntity<DataPointVO>(HttpStatus.FORBIDDEN);
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
        
        return new ResponseEntity<DataPointVO>(vo, headers, HttpStatus.CREATED);
    }
	



	@RequestMapping(method = RequestMethod.DELETE, value = "/{xid}")
    public ResponseEntity<DataPointVO> delete(@PathVariable String xid, HttpServletRequest request) {
		
		//TODO Fix up to use delete by XID?
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		if (vo == null) {
            return new ResponseEntity<DataPointVO>(HttpStatus.NOT_FOUND);
        }
		
		//Check permissions
        User user = Common.getUser(request);
    	try{
    		//TODO Is this the correct permission to check?
    		if(!Permissions.hasDataPointReadPermission(user, vo))
    			return new ResponseEntity<DataPointVO>(HttpStatus.FORBIDDEN);
    	}catch(PermissionException e){
    		return new ResponseEntity<DataPointVO>(HttpStatus.FORBIDDEN);
    	}
		
		
		DataPointDao.instance.delete(vo.getId());
        
		return new ResponseEntity<DataPointVO>(vo, HttpStatus.OK);
    }
	
	
}
