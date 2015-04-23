/*
Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
@author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.List;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * A permission definition allows a module to define a single permission string. The enforcement of this permission is
 * the responsibility of the module itself. The core will present a text box on the system settings permissions page
 * to allow for the editing of the permission string.
 * 
 * The permission string value will be stored in the system settings table using the permission type name as the key.
 * 
 * @author Matthew Lohbihler
 */
abstract public class PermissionDefinition extends ModuleElementDefinition {
    /**
     * A reference to a human readable and translatable brief description of the permission. Key references values in
     * i18n.properties files. Descriptions are used in the system settings permission section and so should be as brief
     * as possible.
     * 
     * @return the reference key to the permission description.
     */
    abstract public String getPermissionKey();

    /**
     * An internal identifier for this type of permission. Must be unique within an MA instance, and is recommended
     * to have the form "&lt;moduleName&gt;.&lt;permissionName&gt;" so as to be unique across all modules.
     * 
     * This will be used in the system settings table to store the permission's groups
     * 
     * @return the permission type name.
     */
    abstract public String getPermissionTypeName();
    
    /**
     * Offers the implementer the option to add default groups to the permission when the module is upgraded 
     * or installed.
     * @return - List of groups to assign to permission
     */
    public List<String> getDefaultGroups(){
    	return null;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleElementDefinition#postDatabase()
     */
    @Override
    public void install() {
    	List<String> defaultGroups = getDefaultGroups();
    	if(defaultGroups != null){
    		String newGroups = new String();
    		String groups = SystemSettingsDao.getValue(this.getPermissionTypeName());
    		//Do we have any of the groups
    		if(groups != null){		
    			for(String defaultGroup : defaultGroups){
    				if(!Permissions.permissionContains(defaultGroup, groups)){
   						newGroups += "," + defaultGroup;
    				}
    			}
    		}else{
    			//No groups so we can safely add ours
    			for(String defaultGroup : defaultGroups){
    				if(newGroups.isEmpty())
    					newGroups += defaultGroup;
    				else
    					newGroups += "," + defaultGroup;
    			}
    		}
    		if(!newGroups.isEmpty()){
				SystemSettingsDao systemSettingsDao = new SystemSettingsDao();
				if(groups != null)
					newGroups = groups + newGroups;
				systemSettingsDao.setValue(getPermissionTypeName(), newGroups);
			}
    	}
    }
}
