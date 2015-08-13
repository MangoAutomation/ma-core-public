package com.serotonin.m2m2.web.taglib;

import java.util.Set;

import javax.servlet.jsp.jstl.core.ConditionalTagSupport;

import org.apache.commons.lang.StringUtils;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * Useful to output information if a user has none of the permissions from the set of given permissions
 * @author Terry Packer
 *
 */
public class HasNoPermissionTag extends ConditionalTagSupport {
    private static final long serialVersionUID = 1L;

    private User user;
    private String permissions;
    private String permissionsDefinitions;
    
    /**
     * Set the user to test permissions against
     * @param user
     */
    public void setUser(User user){
    	this.user = user;
    }
    
    /**
     * Comma separated list of permissions, only 1 permission is required 
     * by the user.
     * @param permissions
     */
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    /**
     * Comma separated list of permission definitions, only 1 permission is required 
     * by the user.
     * @param definitions
     */
    public void setPermissionDefinitions(String definitions){
    	this.permissionsDefinitions = definitions;
    }
    
    @Override
    protected boolean condition() {
    	
    	if(user.isAdmin())
    		return false;
    	
    	//Check to see if we have definitions
    	if(this.permissionsDefinitions != null){
    		Set<String> permissionDefs = Permissions.explodePermissionGroups(permissionsDefinitions);
    		for(String def : permissionDefs){
    			String groups = SystemSettingsDao.getValue(def);
    			if(!StringUtils.isEmpty(groups)){
    				if(this.permissions == null){
    					permissions = groups;
    				}else{
    					permissions += "," + groups;
    						
    				}
    			}
    		}
    	}
    	
    	if(permissions == null)
    		return true;
    	
        return !Permissions.hasPermission(user, permissions);
    }
}
