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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.model.DataPointModel;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @author Terry Packer
 * 
 */
@Api(value="Data Points", description="Operations on Data points", position=1)
@RestController(value="DataPointRestControllerV1")
@RequestMapping("/v1/dataPoints")
public class DataPointRestController extends MangoRestController<DataPointModel>{

	private static Logger LOG = Logger.getLogger(DataPointRestController.class);
	
	public DataPointRestController(){
		LOG.info("Creating Data Point Rest Controller.");
	}

	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DataPointModel> getAllDataPoints(HttpServletRequest request, 
    		@RequestParam(value="limit", required=false, defaultValue="100")int limit) {

    	List<DataPointVO> dataPoints = DataPointDao.instance.getAll();
        List<DataPointModel> userDataPoints = new ArrayList<DataPointModel>();
        
        //Filter on permissions
        User user = Common.getUser(request);
        for(DataPointVO vo : dataPoints){
        	try{
        		if(Permissions.hasDataPointReadPermission(user, vo)){
        			userDataPoints.add(new DataPointModel(vo));
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
	
	
//	@ApiResponses(value = { 
//			@ApiResponse(code = 200, message = "Ok", response = ResponseEntity.class),
//			@ApiResponse(code = 403, message = "User does not have access"),
//		    @ApiResponse(code = 404, message = "DataPoint not found")
//			})

	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<DataPointModel> getDataPoint(
    		@ApiParam(value = "Valid Data Point XIDs", required = true, allowMultiple = false)
    		@PathVariable String xid, HttpServletRequest request) {

		ProcessResult response = new ProcessResult();
		
        DataPointVO vo = DataPointDao.instance.getByXid(xid);
        if (vo == null) {
    		//TODO Add to messages or extract to superclass
    		response.addMessage(new TranslatableMessage("common.default", "Point Does not exist"));
    		return this.createResponseEntity(response, HttpStatus.NOT_FOUND);
        }

        //Check permissions
        User user = Common.getUser(request);
    	try{
    		if(Permissions.hasDataPointReadPermission(user, vo))
    			return this.createResponseEntity(response, new DataPointModel(vo), HttpStatus.OK);
    		else{
    			//TODO add to translations
    			response.addMessage(new TranslatableMessage("common.default", "Do not have permissions to access point"));
        		return this.createResponseEntity(response, HttpStatus.FORBIDDEN);
    		}
    	}catch(PermissionException e){
    		LOG.error(e.getMessage());
        	response.addMessage(new TranslatableMessage("common.default", e.getMessage()));
    		return this.createResponseEntity(response, HttpStatus.FORBIDDEN);
    		
    	}
    }
	
	/**
	 * Put a data point into the system
	 * @param vo
	 * @param xid
	 * @param builder
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<DataPointModel> updateDataPoint(@PathVariable String xid,
    		DataPointModel model, 
    		UriComponentsBuilder builder, HttpServletRequest request) {

		ProcessResult response = new ProcessResult();
		DataPointVO vo = model.getData();
		
        DataPointVO existingDp = DataPointDao.instance.getByXid(xid);
        if (existingDp == null) {
    		//TODO Add to messages or extract to superclass
    		response.addMessage(new TranslatableMessage("common.default", "Point Does not exist"));
    		return this.createResponseEntity(response, HttpStatus.NOT_FOUND);
        }
        
        //Check permissions
        User user = Common.getUser(request);
    	try{
    		if(!Permissions.hasDataPointReadPermission(user, vo)){
    			//TODO Add to messages or extract to superclass
    			response.addMessage(new TranslatableMessage("common.default", "Do not have permissions to access point"));
        		return this.createResponseEntity(response, HttpStatus.FORBIDDEN);

    		}
    	}catch(PermissionException e){
    		//TODO Add to messages or extract to superclass
			response.addMessage(new TranslatableMessage("common.default", "Do not have permissions to access point"));
    		return this.createResponseEntity(response, HttpStatus.FORBIDDEN);
    	}

        
        //We do not read the XID or ID via JSON
        //TODO One reason to use a custom JSON converter
        vo.setXid(xid);
        vo.setId(existingDp.getId());
        
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
    	URI location = builder.path("/rest/v1/dataPoints/{xid}").buildAndExpand(xid).toUri();
        ResponseEntity<DataPointModel> entity =  this.createResponseEntity(location, response, model, HttpStatus.CREATED);
        return entity;
    }
	



	@RequestMapping(method = RequestMethod.DELETE, value = "/{xid}")
    public ResponseEntity<DataPointModel> delete(@PathVariable String xid, HttpServletRequest request) {
		
		ProcessResult response = new ProcessResult();
		
		//TODO Fix up to use delete by XID?
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		if (vo == null) {
    		//TODO Add to messages or extract to superclass
    		response.addMessage(new TranslatableMessage("common.default", "Point Does not exist"));
    		return this.createResponseEntity(response, HttpStatus.NOT_FOUND);
    	}
		
		//Check permissions
        User user = Common.getUser(request);
    	try{
    		//TODO Is this the correct permission to check?
    		if(!Permissions.hasDataPointReadPermission(user, vo)){
    			response.addMessage(new TranslatableMessage("common.default", "Do not have permissions to access point"));
        		return this.createResponseEntity(response, HttpStatus.FORBIDDEN);
    		}
    	}catch(PermissionException e){
    		response.addMessage(new TranslatableMessage("common.default", "Do not have permissions to access point"));
    		return this.createResponseEntity(response, HttpStatus.FORBIDDEN);
    	}
		
		try{
			DataPointDao.instance.delete(vo.getId());
		}catch(Exception e){
			LOG.error(e);
			response.addMessage(new TranslatableMessage("common.default", e.getMessage()));
        	
        	return this.createResponseEntity(response);
		}
		return this.createResponseEntity(response, new DataPointModel(vo), HttpStatus.OK);
    }
	
	
}
