/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.permission;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * @author Matthew Lohbihler
 * 
 */
public class Permissions {
    public interface DataPointAccessTypes {
        int NONE = 0;
        int READ = 1;
        int SET = 2;
        int DATA_SOURCE = 3;
        int ADMIN = 4;
    }
    
    public static final ExportCodes ACCESS_TYPE_CODES = new ExportCodes();
    static{
    	ACCESS_TYPE_CODES.addElement(DataPointAccessTypes.NONE, "NONE");
    	ACCESS_TYPE_CODES.addElement(DataPointAccessTypes.READ, "READ");
    	ACCESS_TYPE_CODES.addElement(DataPointAccessTypes.SET, "SET");
    	ACCESS_TYPE_CODES.addElement(DataPointAccessTypes.DATA_SOURCE, AuditEventType.TYPE_DATA_SOURCE);
    	ACCESS_TYPE_CODES.addElement(DataPointAccessTypes.ADMIN, "ADMIN");
    }
    
    private Permissions() {
        // no op
    }

    //
    //
    // Valid user
    //
    public static void ensureValidUser(User user) throws PermissionException {
        if (user == null)
            throw new PermissionException(new TranslatableMessage("permission.exception.notAuthenticated"), null);
        if (user.isDisabled())
            throw new PermissionException(new TranslatableMessage("permission.exception.userIsDisabled"), user);
    }

	public static boolean isValidUser(User user){
		if (user == null)
            return false;
		else if (user.isDisabled())
           return false;
		else return true;
	}
    
    //
    //
    // Administrator
    //
    public static boolean hasAdmin(User user) throws PermissionException {
        ensureValidUser(user);
        return user.isAdmin();
    }

    public static void ensureAdmin(User user) throws PermissionException {
        if (!hasAdmin(user))
            throw new PermissionException(new TranslatableMessage("permission.exception.mustBeAdmin"), user);
    }

    //
    //
    // Data source admin
    //
    public static void ensureDataSourcePermission(User user, int dsId) throws PermissionException {
        ensureDataSourcePermission(user, DataSourceDao.instance.get(dsId));
    }

    public static void ensureDataSourcePermission(User user, DataSourceVO<?> ds) throws PermissionException {
        if (!hasDataSourcePermission(user, ds))
            throw new PermissionException(new TranslatableMessage("permission.exception.editDataSource", user.getUsername()), user);
    }

    public static boolean hasDataSourcePermission(User user, int dsId) throws PermissionException {
        return hasDataSourcePermission(user, DataSourceDao.instance.get(dsId));
    }

    public static boolean hasDataSourcePermission(User user, DataSourceVO<?> ds) throws PermissionException {
        if (hasAdmin(user))
            return true;
        return permissionContains(ds.getEditPermission(), user.getPermissions());
    }

    public static void ensureDataSourcePermission(User user) throws PermissionException {
        if (!hasDataSourcePermission(user))
            throw new PermissionException(new TranslatableMessage("permission.exception.editAnyDataSource", user.getUsername()), user);
    }

    public static boolean hasDataSourcePermission(User user) throws PermissionException {
        ensureValidUser(user);
        String p = SystemSettingsDao.instance.getValue(SystemSettingsDao.PERMISSION_DATASOURCE, "");
        return hasPermission(user, p);
    }

    public static boolean hasDataSourcePermission(String userPermissions, DataSourceVO<?> ds){
        if(hasPermission(ds.getEditPermission(), userPermissions))
        	return true;
        else
        	return false;
    }
    
    
    //
    //
    // Data point access
    //
    public static void ensureDataPointReadPermission(User user, IDataPoint point) throws PermissionException {
        if (!hasDataPointReadPermission(user, point))
            throw new PermissionException(new TranslatableMessage("permission.exception.readDataPoint", user.getUsername()), user);
    }

    public static boolean hasDataPointReadPermission(User user, IDataPoint point) throws PermissionException {
        if (hasPermission(user, point.getReadPermission()))
            return true;
        return hasDataPointSetPermission(user, point);
    }

    public static boolean hasDataPointReadPermission(String userPermissions, IDataPoint point){
    	if(hasPermission(point.getReadPermission(), userPermissions) || 
    	   permissionContains(point.getSetPermission(), userPermissions)) //No need to recheck admin
    		return true;
    	String dsPermission = DataSourceDao.instance.getEditPermission(point.getDataSourceId());
    	if (permissionContains(dsPermission, userPermissions))
            return true;
        else
        	return false;
    }
    
    public static void ensureDataPointSetPermission(User user, DataPointVO point) throws PermissionException {
        if (!point.getPointLocator().isSettable())
            throw new ShouldNeverHappenException("Point is not settable");
        if (!hasDataPointSetPermission(user, point))
            throw new PermissionException(new TranslatableMessage("permission.exception.setDataPoint", user.getUsername()), user);
    }

    public static boolean hasDataPointSetPermission(User user, IDataPoint point) throws PermissionException {
        if (hasPermission(user, point.getSetPermission()))
            return true;
        String dsPermission = DataSourceDao.instance.getEditPermission(point.getDataSourceId());
        return permissionContains(dsPermission, user.getPermissions());
    }

    /**
     * Returns true or Exception
     * @param userPermissions
     * @param point
     * @return
     */
    public static boolean hasDataPointSetPermission(String userPermissions, IDataPoint point){
        if(hasPermission(point.getSetPermission(), userPermissions))
        	return true;
    	String dsPermission = DataSourceDao.instance.getEditPermission(point.getDataSourceId());
    	if (permissionContains(dsPermission, userPermissions))
            return true;
        else
        	return false;
    }
    
    public static int getDataPointAccessType(User user, IDataPoint point) {
        if (!isValidUser(user))
            return DataPointAccessTypes.NONE;
        if (user.isAdmin())
            return DataPointAccessTypes.ADMIN;

        String dsPermission = DataSourceDao.instance.getEditPermission(point.getDataSourceId());
        if (permissionContains(dsPermission, user.getPermissions()))
            return DataPointAccessTypes.DATA_SOURCE;

        if (permissionContains(point.getSetPermission(), user.getPermissions()))
            return DataPointAccessTypes.SET;
        if (permissionContains(point.getReadPermission(), user.getPermissions()))
            return DataPointAccessTypes.READ;
        return DataPointAccessTypes.NONE;
    }

    //
    //
    // Event access
    //
    public static boolean hasEventTypePermission(User user, EventType eventType) {
        return user.isAdmin() || eventType.hasPermission(user);
    }

    public static void ensureEventTypePermission(User user, EventType eventType) throws PermissionException {
        if (!hasEventTypePermission(user, eventType))
            throw new PermissionException(new TranslatableMessage("permission.exception.event", user.getUsername()), user);
    }

    public static void ensureEventTypePermission(User user, EventTypeVO eventType) throws PermissionException {
        ensureEventTypePermission(user, eventType.createEventType());
    }

    //
    // Utility
    //
    public static boolean hasPermission(User user, String query) {
        if (hasAdmin(user))
            return true;
        return permissionContains(query, user.getPermissions());
    }
    
    /**
     * Utility to check for a user permission in a list of permissions
     * that ensures Super Admin access to everything
     * @param query
     * @param userPermissions
     * @return
     */
    public static boolean hasPermission(String query, String userPermissions){
    	if (permissionContains(SuperadminPermissionDefinition.GROUP_NAME, userPermissions))
            return true;
        return permissionContains(query, userPermissions);
    }

    /**
     * Checks if the given query matches the given group list. Each is a comma-delimited list of tags, where if any
     * tag in the query string matches any tag in the groups string, true is returned. In other words, if there is
     * any intersection in the tags, permission is granted.
     * 
     * @param query
     *            the granted permission tags
     * @param groups
     *            the owned permission tags
     * @return true if permission is granted, false otherwise.
     */
    public static boolean permissionContains(String query, String groups) {
        if (StringUtils.isEmpty(query) || StringUtils.isEmpty(groups))
            return false;

        String[] queryParts = query.split(",");
        String[] groupParts = groups.split(",");
        for (String queryPart : queryParts) {
            if (StringUtils.isEmpty(queryPart))
                continue;
            for (String groupPart : groupParts) {
                if (StringUtils.equals(queryPart.trim(), groupPart.trim()))
                    return true;
            }
        }

        return false;
    }

    /**
     * Provides detailed information on the permission provided to a user for a given query string.
     */
    public static PermissionDetails getPermissionDetails(String query, User user) {
        PermissionDetails d = new PermissionDetails(user.getUsername());

        d.setAdmin(user.isAdmin());

        if (!StringUtils.isEmpty(user.getPermissions())) {
            for (String s : user.getPermissions().split(",")) {
                if (!StringUtils.isEmpty(s))
                    d.addGroup(s);
            }

            if (!StringUtils.isEmpty(query)) {
                for (String queryPart : query.split(",")) {
                    if (StringUtils.isEmpty(queryPart))
                        continue;

                    for (String groupPart : d.getAllGroups()) {
                        if (StringUtils.equals(queryPart.trim(), groupPart.trim()))
                            d.addMatchingGroup(groupPart);
                    }
                }
            }
        }

        return d;
    }

    /**
     * Provides detailed information on the permission provided to a user for a given query string.
     * 
     * Also filters out any permissions that are not part of the limitPermissions
     * so not all permissions or users are viewable.
     * 
     * If the currentUser is an admin then everything is visible
     *
     * @param currentUser - user to limit details of view to thier permissions groups
     * @param query - Any permissions to show as already added in the UI
     * @param user - User for whom to check permissions
     * @return Null if no permissions align else the permissions details with only the viewable groups
     */
    public static PermissionDetails getPermissionDetails(User currentUser, String query, User user) {
        PermissionDetails d = new PermissionDetails(user.getUsername());

        d.setAdmin(user.isAdmin());

        //Add any matching groups
        if (!StringUtils.isEmpty(user.getPermissions())) {
        	if(currentUser.isAdmin()){
        		//Add all groups
        		for (String s : user.getPermissions().split(",")) {
                    if (!StringUtils.isEmpty(s))
                        d.addGroup(s);
                }
        	}else{
	        	Set<String> matching = findMatchingPermissions(currentUser.getPermissions(), user.getPermissions());
	        	for(String match : matching){
	        		d.addGroup(match);
	        	}
        	}

            if (!StringUtils.isEmpty(query)) {
                for (String queryPart : query.split(",")) {
                    if (StringUtils.isEmpty(queryPart))
                        continue;

                    for (String groupPart : d.getAllGroups()) {
                        if (StringUtils.equals(queryPart.trim(), groupPart.trim()))
                            d.addMatchingGroup(groupPart);
                    }
                }
            }
        }

        if(d.getAllGroups().size() == 0)
        	return null;
        
        return d;
    }
    
    public static Set<String> explodePermissionGroups(String groups) {
        Set<String> set = new HashSet<>();

        if (!StringUtils.isEmpty(groups)) {
            for (String s : groups.split(",")) {
                if (!StringUtils.isEmpty(s)){
                	s = s.trim();
                    set.add(s);
                }
            }
        }

        return set;
    }

	/**
	 * Find any permissions that are not granted
	 * 
	 * @param permissions - Permissions of the item
	 * @param userPermissions - Granted permissions
	 * @return
	 */
	public static Set<String> findInvalidPermissions(
			String permissions, String userPermissions) {
		
		Set<String> notGranted = new HashSet<String>();
		Set<String> itemPermissions = explodePermissionGroups(permissions);
		Set<String> grantedPermissions = explodePermissionGroups(userPermissions);
		
		for(String itemPermission : itemPermissions){
			if(!grantedPermissions.contains(itemPermission))
				notGranted.add(itemPermission);
		}
		
		return notGranted;
	}
	
	/**
	 * Find any permissions that are not granted
	 * 
	 * @param permissions - Permissions of the item
	 * @param userPermissions - Granted permissions
	 * @return
	 */
	public static Set<String> findMatchingPermissions(
			String permissions, String userPermissions) {
		
		Set<String> matching = new HashSet<String>();
		Set<String> itemPermissions = explodePermissionGroups(permissions);
		Set<String> grantedPermissions = explodePermissionGroups(userPermissions);
		
		for(String itemPermission : itemPermissions){
			if(grantedPermissions.contains(itemPermission))
				matching.add(itemPermission);
		}
		return matching;
	}
	
	/**
	 * Create a comma separated list from a set of permission groups
	 * @param groups
	 * @return
	 */
	public static String implodePermissionGroups(Set<String> groups){
		String groupsList = new String();
		for(String p : groups){
			if(groupsList.isEmpty())
				groupsList += p;
			else
				groupsList += ("," + p);
		}
		return groupsList;
	}
	
	
	/**
	 * Validate permissions that are being set on an item.
	 * 
	 * Any permissions that the User does not have are invalid unless that user is an admin.
	 * 
	 * @param itemPermissions
	 * @param user
	 * @param response
	 * @param contextKey - UI Element ID
	 */
	public static void validateAddedPermissions(String itemPermissions, User user, ProcessResult response, String contextKey){
		
		if(user == null){
			response.addContextualMessage(contextKey, "validate.invalidPermission","No User Found");
			return;
		}
		if(!user.isAdmin()){
			//if permission is empty then don't bother checking
			if(!itemPermissions.isEmpty()){
				//Determine if any of the permissions are unavailable to us
				Set<String> invalid = findInvalidPermissions(itemPermissions, user.getPermissions());
				if(invalid.size() > 0){
					String notGranted = implodePermissionGroups(invalid);
					response.addContextualMessage(contextKey, "validate.invalidPermission", notGranted);
				}
			}
		}
		
	}


	
}
