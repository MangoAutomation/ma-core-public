/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
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
		LOG.info("Creating DS Rest Controller");
	}
	private static Log LOG = LogFactory.getLog(DataSourceRestController.class);
	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<AbstractDataSourceModel<?>> getAllDataSources() {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all data sources");
        List<DataSourceVO<?>> dataSources = DaoRegistry.dataSourceDao.getAll();
        List<AbstractDataSourceModel<?>> models = new ArrayList<AbstractDataSourceModel<?>>();
        for(DataSourceVO<?> ds : dataSources)
        	models.add(ds.getModel());
        return models;
    }
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public ResponseEntity<AbstractDataSourceModel<?>> getDataSource(@PathVariable String xid) {

        DataSourceVO<?> vo = DaoRegistry.dataSourceDao.getByXid(xid);

        if (vo == null) {
            return new ResponseEntity<AbstractDataSourceModel<?>>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<AbstractDataSourceModel<?>>(vo.getModel(), HttpStatus.OK);
    }
	
	
	
	/**
	 * Put a data source into the system
	 * @param vo
	 * @param xid
	 * @param builder
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<AbstractDataSourceModel<?>> updateDataPoint(
    		@PathVariable String xid,
    		@RequestBody AbstractDataSourceModel<?> model, 
    		UriComponentsBuilder builder, 
    		HttpServletRequest request) {

		RestProcessResult<AbstractDataSourceModel<?>> result = new RestProcessResult<AbstractDataSourceModel<?>>(HttpStatus.OK);

		User user = this.checkUser(request, result);
        if(result.isOk()){
        	
			DataSourceVO<?> vo = model.getData();
			
	        DataSourceVO<?> existing = DaoRegistry.dataSourceDao.getByXid(xid);
	        if (existing == null) {
	    		result.addRestMessage(getDoesNotExistMessage());
	    		return result.createResponseEntity();
	        }
	        
	        //Check permissions
	    	try{
	    		if(!Permissions.hasDataSourcePermission(user, existing.getId())){
	    			result.addRestMessage(getUnauthorizedMessage());
	        		return result.createResponseEntity();
	
	    		}
	    	}catch(PermissionException e){
	    		LOG.warn(e.getMessage(), e);
	    		result.addRestMessage(getUnauthorizedMessage());
        		return result.createResponseEntity();
        	}
	
	        vo.setId(existing.getId());
	        ProcessResult validation = new ProcessResult();
	        vo.validate(validation);
	        
	        if(validation.getHasMessages()){
	        	result.addRestMessage(model.addValidationMessages(validation));
	        	return result.createResponseEntity(model); 
	        }else{
	        	LOG.info("Could save DS NOW");
	            Common.runtimeManager.saveDataSource(vo);
	        }
	        
	        //Put a link to the updated data in the header?
	    	URI location = builder.path("/rest/v1/dataSources/{xid}").buildAndExpand(xid).toUri();
	    	result.addRestMessage(getResourceCreatedMessage(location));
	        return result.createResponseEntity(model);
        }
        //Not logged in
        return result.createResponseEntity();
    }
	
}
