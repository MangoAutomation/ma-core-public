/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.permission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.spring.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
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
    public static void ensureValidPermissionHolder(PermissionHolder user) throws PermissionException {
        if (user == null)
            throw new PermissionException(new TranslatableMessage("permission.exception.notAuthenticated"), null);
        if (user.isPermissionHolderDisabled())
            throw new PermissionException(new TranslatableMessage("permission.exception.userIsDisabled"), user);
    }

    public static boolean isValidPermissionHolder(PermissionHolder user){
        if (user == null)
            return false;
        else if (user.isPermissionHolderDisabled())
            return false;
        else return true;
    }

    //
    //
    // Administrator
    //
    public static boolean hasAdminPermission(PermissionHolder user) throws PermissionException {
        return hasSinglePermission(user, SuperadminPermissionDefinition.GROUP_NAME);
    }

    public static void ensureHasAdminPermission(PermissionHolder user) throws PermissionException {
        if (!hasAdminPermission(user))
            throw new PermissionException(new TranslatableMessage("permission.exception.mustBeAdmin"), user);
    }

    //
    //
    // Data source admin
    //
    public static void ensureDataSourcePermission(PermissionHolder user, int dsId) throws PermissionException {
        ensureDataSourcePermission(user, DataSourceDao.instance.get(dsId));
    }

    public static void ensureDataSourcePermission(PermissionHolder user, DataSourceVO<?> ds) throws PermissionException {
        if (!hasDataSourcePermission(user, ds))
            throw new PermissionException(new TranslatableMessage("permission.exception.editDataSource", user.getPermissionHolderName()), user);
    }

    public static boolean hasDataSourcePermission(PermissionHolder user, int dsId) throws PermissionException {
        String dsPermission = DataSourceDao.instance.getEditPermission(dsId);
        return hasAnyPermission(user, explodePermissionGroups(dsPermission));
    }

    public static boolean hasDataSourcePermission(PermissionHolder user, DataSourceVO<?> ds) throws PermissionException {
        return hasAnyPermission(user, explodePermissionGroups(ds.getEditPermission()));
    }

    public static void ensureDataSourcePermission(PermissionHolder user) throws PermissionException {
        if (!hasDataSourcePermission(user))
            throw new PermissionException(new TranslatableMessage("permission.exception.editAnyDataSource", user.getPermissionHolderName()), user);
    }

    public static boolean hasDataSourcePermission(PermissionHolder user) throws PermissionException {
        String p = SystemSettingsDao.instance.getValue(SystemSettingsDao.PERMISSION_DATASOURCE, "");
        return hasAnyPermission(user, explodePermissionGroups(p));
    }

    //
    //
    // Data point access
    //
    public static void ensureDataPointReadPermission(PermissionHolder user, IDataPoint point) throws PermissionException {
        if (!hasDataPointReadPermission(user, point))
            throw new PermissionException(new TranslatableMessage("permission.exception.readDataPoint", user.getPermissionHolderName()), user);
    }

    public static boolean hasDataPointReadPermission(PermissionHolder user, IDataPoint point) throws PermissionException {
        if (hasAnyPermission(user, explodePermissionGroups(point.getReadPermission())))
            return true;
        return hasDataPointSetPermission(user, point);
    }

    public static void ensureDataPointSetPermission(PermissionHolder user, DataPointVO point) throws PermissionException {
        if (!hasDataPointSetPermission(user, point))
            throw new PermissionException(new TranslatableMessage("permission.exception.setDataPoint", user.getPermissionHolderName()), user);
    }

    public static boolean hasDataPointSetPermission(PermissionHolder user, IDataPoint point) throws PermissionException {
        if (hasAnyPermission(user, explodePermissionGroups(point.getSetPermission())))
            return true;

        return hasDataSourcePermission(user, point.getDataSourceId());
    }

    public static int getDataPointAccessType(PermissionHolder user, IDataPoint point) {
        if (!isValidPermissionHolder(user))
            return DataPointAccessTypes.NONE;
        if (hasAdminPermission(user))
            return DataPointAccessTypes.ADMIN;
        if (hasDataSourcePermission(user, point.getDataSourceId()))
            return DataPointAccessTypes.DATA_SOURCE;
        if (hasDataPointSetPermission(user, point))
            return DataPointAccessTypes.SET;
        if (hasDataPointReadPermission(user, point))
            return DataPointAccessTypes.READ;
        return DataPointAccessTypes.NONE;
    }

    //
    //
    // Event access
    //
    public static boolean hasEventTypePermission(PermissionHolder user, EventType eventType) {
        return hasAdminPermission(user) || eventType.hasPermission(user);
    }

    public static void ensureEventTypePermission(PermissionHolder user, EventType eventType) throws PermissionException {
        if (!hasEventTypePermission(user, eventType))
            throw new PermissionException(new TranslatableMessage("permission.exception.event", user.getPermissionHolderName()), user);
    }

    public static void ensureEventTypePermission(PermissionHolder user, EventTypeVO eventType) throws PermissionException {
        ensureEventTypePermission(user, eventType.createEventType());
    }

    //
    // Utility
    //

    /**
     * Try not to use this, use hasAnyPermission() instead.
     *
     * @param user
     * @param requiredPermissions comma separated list of permissions
     * @return true if user holds any of the required permissions
     */
    public static boolean hasPermission(PermissionHolder user, String requiredPermissions) {
        return hasAnyPermission(user, explodePermissionGroups(requiredPermissions));
    }

    /**
     * Provides detailed information on the permission held by a user for a given query string.
     *
     * Also filters out any permissions that are not held by currentUser,
     * so not all permissions or users are viewable.
     *
     * If the currentUser is an admin then everything is visible
     *
     * @param currentUser - user to limit details of view to their permissions groups
     * @param query - Any permissions to show as already added in the UI
     * @param user - PermissionHolder for whom to check permissions
     * @return Null if no permissions align else the permissions details with only the viewable groups
     */
    public static PermissionDetails getPermissionDetails(PermissionHolder currentUser, String query, PermissionHolder user) {
        PermissionDetails d = new PermissionDetails(user.getPermissionHolderName());
        d.setAdmin(hasAdminPermission(user));

        boolean currentUserAdmin = hasAdminPermission(currentUser);
        Set<String> querySet = explodePermissionGroups(query);

        // Add any matching groups
        for (String permission : user.getPermissionsSet()) {
            if (currentUserAdmin || hasSinglePermission(currentUser, permission)) {
                d.addGroup(permission);

                if (querySet.contains(permission)) {
                    d.addMatchingGroup(permission);
                }
            }
        }

        if (d.getAllGroups().size() == 0)
            return null;

        return d;
    }

    public static Set<String> explodePermissionGroups(String groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> set = new HashSet<>();
        for (String s : groups.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) {
                set.add(s);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Returns all permissions in the string that the user does not hold.
     *
     * @param user
     * @param permissions
     * @return
     */
    public static Set<String> findInvalidPermissions(PermissionHolder user, String permissions) {
        Set<String> notHeld = new HashSet<String>();
        Set<String> itemPermissions = explodePermissionGroups(permissions);

        for (String itemPermission : itemPermissions) {
            if (!hasSinglePermission(user, itemPermission)) {
                notHeld.add(itemPermission);
            }
        }

        return notHeld;
    }

    /**
     * Returns all permissions in the string that the user holds.
     *
     * @param user
     * @param permissions
     * @return
     */
    public static Set<String> findMatchingPermissions(PermissionHolder user, String permissions) {
        Set<String> matching = new HashSet<String>();
        Set<String> itemPermissions = explodePermissionGroups(permissions);

        for (String itemPermission : itemPermissions) {
            if (hasSinglePermission(user, itemPermission)) {
                matching.add(itemPermission);
            }
        }

        return matching;
    }

    /**
     * Create a comma separated list from a set of permission groups
     * @param groups
     * @return
     */
    public static String implodePermissionGroups(Set<String> groups) {
        checkPermissionSet(groups);
        return String.join(",", groups.stream().map(group -> group.trim()).collect(Collectors.toSet()));
    }

    /**
     * Validate permissions that are being set on an item.
     *
     * Any permissions that the PermissionHolder does not have are invalid unless that user is an admin.
     *
     * @param itemPermissions
     * @param user
     * @param response
     * @param contextKey - UI Element ID
     */
    public static void validateAddedPermissions(String itemPermissions, PermissionHolder user, ProcessResult response, String contextKey) {
        if (user == null) {
            response.addContextualMessage(contextKey, "validate.invalidPermission", "No user found");
            return;
        }

        Set<String> invalid = findInvalidPermissions(user, itemPermissions);
        if (invalid.size() > 0) {
            String notGranted = implodePermissionGroups(invalid);
            response.addContextualMessage(contextKey, "validate.invalidPermission", notGranted);
        }
    }

    public static boolean hasSinglePermission(PermissionHolder user, String requiredPermission) {
        ensureValidPermissionHolder(user);

        Set<String> heldPermissions = user.getPermissionsSet();
        return containsSinglePermission(heldPermissions, requiredPermission);
    }

    public static boolean hasAnyPermission(PermissionHolder user, Set<String> requiredPermissions) {
        ensureValidPermissionHolder(user);

        Set<String> heldPermissions = user.getPermissionsSet();
        return containsAny(heldPermissions, requiredPermissions);
    }

    public static boolean hasAllPermissions(PermissionHolder user, Set<String> requiredPermissions) {
        ensureValidPermissionHolder(user);

        Set<String> heldPermissions = user.getPermissionsSet();
        return containsAll(heldPermissions, requiredPermissions);
    }

    public static void ensureHasSinglePermission(PermissionHolder user, String requiredPermission) {
        if (!hasSinglePermission(user, requiredPermission)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    public static void ensureHasAnyPermission(PermissionHolder user, Set<String> requiredPermissions) {
        if (!hasAnyPermission(user, requiredPermissions)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    public static void ensureHasAllPermissions(PermissionHolder user, Set<String> requiredPermissions) {
        if (!hasAllPermissions(user, requiredPermissions)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    private static boolean containsSinglePermission(Set<String> heldPermissions, String requiredPermission) {
        if (heldPermissions.contains(SuperadminPermissionDefinition.GROUP_NAME)) {
            return true;
        }

        // empty permissions string indicates that only superadmins are allowed access
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return false;
        }

        return heldPermissions.contains(requiredPermission);
    }

    private static boolean containsAll(Set<String> heldPermissions, Set<String> requiredPermissions) {
        checkPermissionSet(requiredPermissions);

        if (heldPermissions.contains(SuperadminPermissionDefinition.GROUP_NAME)) {
            return true;
        }

        // empty permissions string indicates that only superadmins are allowed access
        if (requiredPermissions.isEmpty()) {
            return false;
        }

        return heldPermissions.containsAll(requiredPermissions);
    }

    private static boolean containsAny(Set<String> heldPermissions, Set<String> requiredPermissions) {
        checkPermissionSet(requiredPermissions);

        if (heldPermissions.contains(SuperadminPermissionDefinition.GROUP_NAME)) {
            return true;
        }

        // empty permissions string indicates that only superadmins are allowed access
        if (requiredPermissions.isEmpty()) {
            return false;
        }

        for (String requiredPermission : requiredPermissions) {
            if (heldPermissions.contains(requiredPermission)) {
                return true;
            }
        }

        return false;
    }

    private static void checkPermissionSet(Set<String> requiredPermissions) {
        Objects.requireNonNull(requiredPermissions, "Permission set cannot be null");

        for (String requiredPermission : requiredPermissions) {
            if (requiredPermission == null || requiredPermission.isEmpty()) {
                throw new IllegalArgumentException("Permission string in set cannot be null or empty");
            }
        }
    }
}
