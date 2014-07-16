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
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.hierarchy.PointFolder;
import com.serotonin.m2m2.vo.hierarchy.PointHierarchy;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Terry Packer
 *
 */
@Controller
@RequestMapping("/v1/hierarchy")
public class PointHierarchyController {

	
	private static Logger LOG = Logger.getLogger(PointHierarchyController.class);
	
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public PointFolder getPointHierarchy(HttpServletRequest request) {
    	if(LOG.isDebugEnabled())
    		LOG.debug("Getting all real time data");
    	
    	//User user = Common.getUser(request);
    	PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true);
    	//TODO User restrictions on view?
    	return ph.getRoot();
    
    }
    
	
	@RequestMapping(method = RequestMethod.GET, value = "/{folderName}")
    public ResponseEntity<PointFolder> getFolder(@PathVariable String folderName, HttpServletRequest request) {
		PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true);
		PointFolder folder = ph.getRoot();
		PointFolder desiredFolder = null;
		
		//TODO user restrictions? User user = Common.getUser(request);
		if(folder.getName().equals(folderName))
			return new ResponseEntity<PointFolder>(folder, HttpStatus.OK);
		else{
			desiredFolder = recursiveFolderSearch(folder, folderName);
		}
		
		if (desiredFolder == null)
            return new ResponseEntity<PointFolder>(HttpStatus.NOT_FOUND);
		else
			return new ResponseEntity<PointFolder>(desiredFolder, HttpStatus.OK);
    }

	/**
	 * Get a path to a folder
	 * @param xid
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/path/{xid}")
    public ResponseEntity<List<String>> getPath(@PathVariable String xid, HttpServletRequest request) {
		PointHierarchy ph = DataPointDao.instance.getPointHierarchy(true);

		User user = Common.getUser(request);
		DataPointVO vo = DataPointDao.instance.getByXid(xid);
		if(vo == null)
			return new ResponseEntity<List<String>>(HttpStatus.NOT_FOUND);
		
		//Check permissions
		try{
			if(!Permissions.hasDataPointReadPermission(user, vo)){
				return new ResponseEntity<List<String>>(HttpStatus.FORBIDDEN);
			}else{
				return new ResponseEntity<List<String>>(ph.getPath(vo.getId()), HttpStatus.OK);
			}
		}catch(PermissionException e){
			return new ResponseEntity<List<String>>(HttpStatus.FORBIDDEN);
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
