/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.permission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
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
 * When this class refers to "permissions" it actually means "roles" or "groups". We refer to system settings that map to a list of roles as "granted permissions".
 */
public class Permissions {

    /**
     * Static Roles:
     * These roles should never be checked for in code. To do so removes the administrator's ability
     * to restrict the action. Instead they serve to guarantee easily granting permissions to all
     * members who get that role by default. They should not need to be manually added to sets as the
     * PermissionsHolder implementor is expected to return them in the getPermissionsSet method
     */
    // The role that all users have by default. By adding this role to a permission all users will have access
    public static final String USER_DEFAULT = "user";

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

    //To check permissions for spaces
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s");

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

    public static boolean isValidPermissionHolder(PermissionHolder user) {
        return !(user == null || user.isPermissionHolderDisabled());
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
        ensureDataSourcePermission(user, DataSourceDao.getInstance().get(dsId));
    }

    public static void ensureDataSourcePermission(PermissionHolder user, DataSourceVO<?> ds) throws PermissionException {
        if (!hasDataSourcePermission(user, ds))
            throw new PermissionException(new TranslatableMessage("permission.exception.editDataSource", user.getPermissionHolderName()), user);
    }

    public static boolean hasDataSourcePermission(PermissionHolder user, int dsId) throws PermissionException {
        String dsPermission = DataSourceDao.getInstance().getEditPermission(dsId);
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
        if(user.hasAdminPermission())
            return true;
        String p = SystemSettingsDao.instance.getValue(SystemSettingsDao.PERMISSION_DATASOURCE);
        if(StringUtils.isNotBlank(p))
            return hasAnyPermission(user, explodePermissionGroups(p));
        else
            return false;
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
        ensureEventTypePermission(user, eventType.getEventType());
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
     * @param itemPermissions
     * @return
     */
    public static Set<String> findInvalidPermissions(PermissionHolder user, Set<String> itemPermissions) {
        Set<String> notHeld = new HashSet<String>();
        for (String itemPermission : itemPermissions) {
            if (!hasSinglePermission(user, itemPermission)) {
                notHeld.add(itemPermission);
            }
        }

        return notHeld;
    }
    /**
     * Returns all permissions in the string that the user does not hold.
     *
     * @param user
     * @param permissions
     * @return
     */
    public static Set<String> findInvalidPermissions(PermissionHolder user, String permissions) {
        Set<String> itemPermissions = explodePermissionGroups(permissions);
        return findInvalidPermissions(user, itemPermissions);
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
     * Validate roles.  This will validate that:
     *
     *   1. the new permissions are non null
     *   2. all new permissions are not empty
     *   3. the new permissions do not contain spaces
     *   (then for non admin/owners)
     *   4. the saving user will at least retain one permission
     *   5. the user cannot not remove an existing permission they do not have
     *   6. the user has all of the new permissions being added
     *
     *   If the saving user is also the owner, then the new permissions need not contain
     *   one of the user's roles
     *
     * @param response - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param holder - the saving permission holder
     * @param savedByOwner - is the saving user the owner of this item (use false if no owner is possible)
     * @param existingPermissions - the currently saved permissions
     * @param newPermissions - the new permissions to validate
     */
    public static void validatePermissions(ProcessResult response, String contextKey, PermissionHolder holder, boolean savedByOwner,
            Set<String> existingPermissions, Set<String> newPermissions) {
        if (holder == null) {
            response.addContextualMessage(contextKey, "validate.userRequired");
            return;
        }

        if(newPermissions == null) {
            response.addContextualMessage(contextKey, "validate.invalidPermissionEmpty");
            return;
        }

        for (String permission : newPermissions) {
            if (permission == null || permission.isEmpty()) {
                response.addContextualMessage(contextKey, "validate.invalidPermissionEmpty");
                return;
            }
        }

        //Ensure there are no spaces
        for(String permission : newPermissions) {
            Matcher matcher = SPACE_PATTERN.matcher(permission);
            if(matcher.find()) {
                response.addContextualMessage(contextKey, "validate.invalidPermissionWithSpace", permission);
                return;
            }
        }

        if(holder.hasAdminPermission())
            return;

        //Ensure the holder has at least one of the new permissions
        if(!savedByOwner && Collections.disjoint(holder.getPermissionsSet(), newPermissions)) {
            response.addContextualMessage(contextKey, "validate.mustRetainPermission");
        }

        if(existingPermissions != null) {
            //Check for permissions being added that the user does not have
            Set<String> added = new HashSet<>(newPermissions);
            added.removeAll(existingPermissions);
            added.removeAll(holder.getPermissionsSet());
            if(added.size() > 0) {
                response.addContextualMessage(contextKey, "validate.invalidPermissionModification", Permissions.implodePermissionGroups(holder.getPermissionsSet()));
            }
            //Check for permissions being removed that the user does not have
            Set<String> removed = new HashSet<>(existingPermissions);
            removed.removeAll(newPermissions);
            removed.removeAll(holder.getPermissionsSet());
            if(removed.size() > 0) {
                response.addContextualMessage(contextKey, "validate.invalidPermissionModification", Permissions.implodePermissionGroups(holder.getPermissionsSet()));
            }
        }
        return;
    }

    public static boolean hasSinglePermission(PermissionHolder user, String requiredPermission) {
        if (!isValidPermissionHolder(user)) return false;

        Set<String> heldPermissions = user.getPermissionsSet();
        return containsSinglePermission(heldPermissions, requiredPermission);
    }

    public static boolean hasAnyPermission(PermissionHolder user, Set<String> requiredPermissions) {
        if (!isValidPermissionHolder(user)) return false;

        Set<String> heldPermissions = user.getPermissionsSet();
        return containsAny(heldPermissions, requiredPermissions);
    }

    public static boolean hasAllPermissions(PermissionHolder user, Set<String> requiredPermissions) {
        if (!isValidPermissionHolder(user)) return false;

        Set<String> heldPermissions = user.getPermissionsSet();
        return containsAll(heldPermissions, requiredPermissions);
    }

    public static void ensureHasSinglePermission(PermissionHolder user, String requiredPermission) {
        if (!hasSinglePermission(user, requiredPermission)) {
            Permissions.ensureValidPermissionHolder(user);
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    public static void ensureHasAnyPermission(PermissionHolder user, Set<String> requiredPermissions) {
        if (!hasAnyPermission(user, requiredPermissions)) {
            Permissions.ensureValidPermissionHolder(user);
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    public static void ensureHasAllPermissions(PermissionHolder user, Set<String> requiredPermissions) {
        if (!hasAllPermissions(user, requiredPermissions)) {
            Permissions.ensureValidPermissionHolder(user);
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Return all the granted permissions a user has.  This is any Permission Definition that the user
     *  has permission for.
     * @param user
     * @return
     */
    public static Set<Permission> getGrantedPermissions(PermissionHolder user){
        Set<Permission> grantedPermissions = new HashSet<>();

        for(Entry<String, PermissionDefinition> def : ModuleRegistry.getPermissionDefinitions().entrySet()) {
            Permission permission = def.getValue().getPermission();
            if(hasGrantedPermission(user, permission))
                grantedPermissions.add(permission);

        }
        return grantedPermissions;
    }

    /**
     * Does this permission holder have access to this permission
     * @param user
     * @param typeName
     * @return
     */
    public static boolean hasGrantedPermission(PermissionHolder user, Permission permission) {
        if(!isValidPermissionHolder(user)) {
            return false;
        } else if (user.hasAdminPermission()) {
            return true;
        }

        return containsAny(user.getPermissionsSet(), permission.getRoles());
    }

    public static boolean hasGrantedPermission(PermissionHolder user, String permissionName) {
        PermissionDefinition def = ModuleRegistry.getPermissionDefinition(permissionName);
        if (def == null) {
            throw new TranslatableRuntimeException(new TranslatableMessage("permissions.accessDeniedInvalidPermission", permissionName));
        }

        return hasGrantedPermission(user, def.getPermission());
    }

    public static void ensureGrantedPermission(PermissionHolder user, Permission permission) {
        if (!hasGrantedPermission(user, permission)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredGrantedPermission", user.getPermissionHolderName()), user);
        }
    }

    public static void ensureGrantedPermission(PermissionHolder user, String permissionName) {
        if (!hasGrantedPermission(user, permissionName)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredGrantedPermission", user.getPermissionHolderName()), user);
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
