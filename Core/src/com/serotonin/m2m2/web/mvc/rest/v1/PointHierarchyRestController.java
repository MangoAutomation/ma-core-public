/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.PointHierarchyModel;
import com.wordnik.swagger.annotations.Api;

/**
 * @author Terry Packer
 *
 */
@Api(value="Point Hierarchy", description="Operations on Point Hierarchy")
@Controller
@RequestMapping("/v1/hierarchy")
public class PointHierarchyRestController extends MangoRestController<PointHierarchyModel>{

	
	private static Log LOG = LogFactory.getLog(PointHierarchyRestController.class);
	
	/**
	 * Get the entire Point Hierarchy
	 * @param request
	 * @return
	 */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PointHierarchyModel> getPointHierarchy(HttpServletRequest request) {

    	RestProcessResult<PointHierarchyModel> result = new RestProcessResult<PointHierarchyModel>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    		
    		if(user.isAdmin()){
		    	PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true);
		    	PointHierarchyModel model = new PointHierarchyModel(ph.getRoot());
		    	return result.createResponseEntity(model);
    		}else{
    			LOG.warn("Non admin user: " + user.getUsername() + " attempted to access point hierarchy");
    			result.addRestMessage(this.getUnauthorizedMessage());
    			return result.createResponseEntity();
    		}
    	}
    	
    	return result.createResponseEntity();
    
    }
    
	/**
	 * Get the folder via a name
	 * @param folderName
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/{folderName}")
    public ResponseEntity<PointHierarchyModel> getFolder(@PathVariable String folderName, HttpServletRequest request) {
		
    	RestProcessResult<PointHierarchyModel> result = new RestProcessResult<PointHierarchyModel>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    		
    		if(user.isAdmin()){
    			PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true);
    			PointFolder folder = ph.getRoot();
    			PointFolder desiredFolder = null;
    			if(folder.getName().equals(folderName))
    				return result.createResponseEntity(new PointHierarchyModel(folder)); 
    			else{
    				desiredFolder = recursiveFolderSearch(folder, folderName);
    			}
    			
    			if (desiredFolder == null){
    				result.addRestMessage(getDoesNotExistMessage());
    	            return result.createResponseEntity();
    			}else
    				return result.createResponseEntity(new PointHierarchyModel(folder)); 

    		}else{
    			LOG.warn("Non admin user: " + user.getUsername() + " attempted to access point hierarchy");
    			result.addRestMessage(this.getUnauthorizedMessage());
    			return result.createResponseEntity();
    		}
    	}
    	
    	return result.createResponseEntity();
    }

	/**
	 * Get a path to a folder
	 * @param xid
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/path/{xid}")
    public ResponseEntity<List<String>> getPath(@PathVariable String xid, HttpServletRequest request) {

    	RestProcessResult<List<String>> result = new RestProcessResult<List<String>>(HttpStatus.OK);
    	
		PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true);

		User user = this.checkUser(request, result);
		if(result.isOk()){
			DataPointVO vo = DataPointDao.instance.getByXid(xid);
			if(vo == null){
				result.addRestMessage(getDoesNotExistMessage());
				return result.createResponseEntity();
			}
			
			//Check permissions
			try{
				if(!Permissions.hasDataPointReadPermission(user, vo)){
					result.addRestMessage(getUnauthorizedMessage());
					return result.createResponseEntity();
				}else{
					return result.createResponseEntity(ph.getPath(vo.getId()));
				}
			}catch(PermissionException e){
				result.addRestMessage(getUnauthorizedMessage());
				return result.createResponseEntity();
			}
		}else{
			return result.createResponseEntity();
		}
    }
	
	
	/**
	 * @param ph
	 * @param folderName
	 * @return
	 */
	private PointFolder recursiveFolderSearch(PointFolder root,
			String folderName) {
		if(root.getName().equals(folderName))
			return root;
		
		for(PointFolder folder : root.getSubfolders()){
			PointFolder found = recursiveFolderSearch(folder, folderName);
			if( found != null)
				return found;
		}
		
		return null;
	}

	
	
}
