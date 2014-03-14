/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.permission;

import javax.servlet.http.HttpServletRequest;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.User;
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

    private Permissions() {
        // no op
    }

    //
    //
    // Valid user
    //
    public static void ensureValidUser() throws PermissionException {
        ensureValidUser(Common.getUser());
    }

    public static void ensureValidUser(HttpServletRequest request) throws PermissionException {
        ensureValidUser(Common.getUser(request));
    }

    public static void ensureValidUser(User user) throws PermissionException {
        if (user == null)
            throw new PermissionException("Not logged in", null);
        if (user.isDisabled())
            throw new PermissionException("User is disabled", user);
    }

    //
    //
    // Administrator
    //
    public static boolean hasAdmin() throws PermissionException {
        return hasAdmin(Common.getUser());
    }

    public static boolean hasAdmin(HttpServletRequest request) throws PermissionException {
        return hasAdmin(Common.getUser(request));
    }

    public static boolean hasAdmin(User user) throws PermissionException {
        ensureValidUser(user);
        return user.isAdmin();
    }

    public static void ensureAdmin() throws PermissionException {
        ensureAdmin(Common.getUser());
    }

    public static void ensureAdmin(HttpServletRequest request) throws PermissionException {
        ensureAdmin(Common.getUser(request));
    }

    public static void ensureAdmin(User user) throws PermissionException {
        if (!hasAdmin(user))
            throw new PermissionException("User is not an administrator", user);
    }

    //
    //
    // Data source admin
    //
    public static void ensureDataSourcePermission(User user, int dataSourceId) throws PermissionException {
        if (!hasDataSourcePermission(user, dataSourceId))
            throw new PermissionException("User does not have permission to data source", user);
    }

    public static void ensureDataSourcePermission(User user) throws PermissionException {
        if (!hasDataSourcePermission(user))
            throw new PermissionException("User does not have permission to any data sources", user);
    }

    public static boolean hasDataSourcePermission(User user, int dataSourceId) throws PermissionException {
        ensureValidUser(user);
        if (user.isAdmin())
            return true;
        return user.getDataSourcePermissions().contains(dataSourceId);
    }

    public static boolean hasDataSourcePermission(User user) throws PermissionException {
        ensureValidUser(user);
        if (user.isAdmin())
            return true;
        return user.getDataSourcePermissions().size() > 0;
    }

    //
    //
    // Data point access
    //
    public static void ensureDataPointReadPermission(User user, IDataPoint point) throws PermissionException {
        if (!hasDataPointReadPermission(user, point))
            throw new PermissionException("User does not have read permission to point", user);
    }

    public static boolean hasDataPointReadPermission(User user, IDataPoint point) throws PermissionException {
        return hasDataPointReadPermission(user, point.getDataSourceId(), point.getId());
    }

    private static boolean hasDataPointReadPermission(User user, int dataSourceId, int dataPointId)
            throws PermissionException {
        if (hasDataSourcePermission(user, dataSourceId))
            return true;
        DataPointAccess a = getDataPointAccess(user, dataPointId);
        if (a == null)
            return false;
        return a.getPermission() == DataPointAccess.READ || a.getPermission() == DataPointAccess.SET;
    }

    public static void ensureDataPointSetPermission(User user, DataPointVO point) throws PermissionException {
        if (!point.getPointLocator().isSettable())
            throw new ShouldNeverHappenException("Point is not settable");
        if (!hasDataPointSetPermission(user, point))
            throw new PermissionException("User does not have set permission to point", user);
    }

    public static boolean hasDataPointSetPermission(User user, IDataPoint point) throws PermissionException {
        if (hasDataSourcePermission(user, point.getDataSourceId()))
            return true;
        DataPointAccess a = getDataPointAccess(user, point.getId());
        if (a == null)
            return false;
        return a.getPermission() == DataPointAccess.SET;
    }

    private static DataPointAccess getDataPointAccess(User user, int dataPointId) {
        for (DataPointAccess a : user.getDataPointPermissions()) {
            if (a.getDataPointId() == dataPointId)
                return a;
        }
        return null;
    }

    public static int getDataPointAccessType(User user, IDataPoint point) {
        if (user == null || user.isDisabled())
            return DataPointAccessTypes.NONE;
        if (user.isAdmin())
            return DataPointAccessTypes.ADMIN;
        if (user.getDataSourcePermissions().contains(point.getDataSourceId()))
            return DataPointAccessTypes.DATA_SOURCE;
        DataPointAccess a = getDataPointAccess(user, point.getId());
        if (a == null)
            return DataPointAccessTypes.NONE;
        if (a.getPermission() == DataPointAccess.SET)
            return DataPointAccessTypes.SET;
        if (a.getPermission() == DataPointAccess.READ)
            return DataPointAccessTypes.READ;
        return DataPointAccessTypes.NONE;
    }

    //
    //
    // Event access
    //
    public static boolean hasEventTypePermission(User user, EventType eventType) {
        if (eventType.getEventType().equals(EventType.EventTypeNames.DATA_POINT))
            return hasDataPointReadPermission(user, eventType.getDataSourceId(), eventType.getDataPointId());
        if (eventType.getEventType().equals(EventType.EventTypeNames.DATA_SOURCE))
            return hasDataPointReadPermission(user, eventType.getDataSourceId(), eventType.getDataPointId());
        return hasAdmin(user);
    }

    public static void ensureEventTypePermission(User user, EventType eventType) throws PermissionException {
        if (!hasEventTypePermission(user, eventType))
            throw new PermissionException("User does not have permission to the view", user);
    }

    public static void ensureEventTypePermission(User user, EventTypeVO eventType) throws PermissionException {
        ensureEventTypePermission(user, eventType.createEventType());
    }
}
