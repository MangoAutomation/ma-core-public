/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.permission.UserRolesDetails;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DataSourcePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventsViewPermissionDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.IDataPoint;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
@Service
public class PermissionService {

    //Permission Types
    public static final String READ = "READ";
    public static final String EDIT = "EDIT";
    public static final String DELETE = "DELETE";
    public static final String SET = "SET";
    public static final String WRITE = "WRITE";
    public static final String SCRIPT = "SCRIPT";

    private final RoleDao roleDao;
    private final DataSourcePermissionDefinition dataSourcePermission;
    private final PermissionHolder systemSuperadmin;
    private final EventsViewPermissionDefinition eventsViewPermission;

    @Autowired
    public PermissionService(RoleDao roleDao,
            @Qualifier(MangoRuntimeContextConfiguration.SYSTEM_SUPERADMIN_PERMISSION_HOLDER)
    PermissionHolder systemSuperadmin,
    DataSourcePermissionDefinition dataSourcePermission,
    EventsViewPermissionDefinition eventsView) {
        this.roleDao = roleDao;
        this.dataSourcePermission = dataSourcePermission;
        this.systemSuperadmin = systemSuperadmin;
        this.eventsViewPermission = eventsView;
    }

    /**
     * Run a command as superadmin
     * @param command
     */
    public void runAsSystemAdmin(Runnable command) {
        runAs(systemSuperadmin, command);
    }

    /**
     * Run a command as superadmin and return a result
     * @param <T>
     * @param command
     * @return
     */
    public <T> T runAsSystemAdmin(Supplier<T> command) {
        return runAs(systemSuperadmin, command);
    }

    public void runAs(PermissionHolder user, Runnable command) {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            newSecurityContext(user);
            command.run();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    public <T> T runAs(PermissionHolder user, Supplier<T> command) {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            newSecurityContext(user);
            return command.get();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    public <T> T runAsCallable(PermissionHolder user, Callable<T> command) throws Exception {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            newSecurityContext(user);
            return command.call();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    /**
     * Creates and sets a new security context for the user
     * @param user
     * @return
     */
    private SecurityContext newSecurityContext(PermissionHolder user) {
        SecurityContext newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(new PreAuthenticatedAuthenticationToken(user, null));
        SecurityContextHolder.setContext(newContext);
        return newContext;
    }

    /**
     * Does this user have the superadmin role?
     * @param holder
     * @return
     */
    public boolean hasAdminRole(PermissionHolder holder) {
        return hasSingleRole(holder, PermissionHolder.SUPERADMIN_ROLE);
    }

    /**
     * Ensure this holder has the superadmin role
     * @param holder
     */
    public void ensureAdminRole(PermissionHolder holder) throws PermissionException {
        if(!hasAdminRole(holder)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", holder.getPermissionHolderName()), holder);
        }
    }

    /**
     * Does this permission holder have access based on the permission's role logic?
     * @param user
     * @param permission
     * @return
     */
    public boolean hasPermission(PermissionHolder user, MangoPermission permission) {
        if (!isValidPermissionHolder(user)) return false;
        if(hasAdminRole(user)) return true;

        for(Set<Role> group : permission.getRoles()) {
            if(hasAllRoles(user, group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ensure the user has a role defined in this permission
     * @param user
     * @param permission
     */
    public void ensurePermission(PermissionHolder user, MangoPermission permission) {
        if (!hasPermission(user, permission)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredGrantedPermission", user != null ? user.getPermissionHolderName() : ""), user);
        }
    }

    /**
     * Does this user have any roles assigned to this permission?
     * @param definition
     * @param holder
     * @return
     */
    public boolean isGrantedPermission(PermissionDefinition definition, PermissionHolder holder) {
        return hasPermission(holder, definition.getPermission());
    }

    /**
     * Return all the granted permissions a user has.  This is any Permission Definition that the user
     *  has permission for.
     *
     *  TODO Mango 4.0 all VO types too, i.e. dataPoint.read (these are not definitions)?
     *
     * @param holder
     * @return
     */
    public Set<String> getGrantedPermissions(PermissionHolder holder) {
        Set<String> grantedPermissions = new HashSet<>();
        if(isValidPermissionHolder(holder)) {
            for(Entry<String, PermissionDefinition> def : ModuleRegistry.getPermissionDefinitions().entrySet()) {
                if(hasAdminRole(holder) || def.getValue().hasPermission(holder)) {
                    grantedPermissions.add(def.getValue().getPermissionTypeName());
                }
            }
        }
        return grantedPermissions;
    }

    /**
     * Does this permission holder have any role for the permission
     *  type on this vo?
     *
     * @param holder
     * @param vo
     * @param permissionType
     * @return
     */
    public boolean hasPermission(PermissionHolder holder, AbstractVO vo, String permissionType) {
        MangoPermission permission = roleDao.getPermission(vo, permissionType);
        return hasPermission(holder, permission);
    }

    /**
     * Ensure this user have the global data source permission
     * @param user
     * @throws PermissionException
     */
    public void ensureDataSourcePermission(PermissionHolder user) throws PermissionException {
        if (!hasDataSourcePermission(user))
            throw new PermissionException(new TranslatableMessage("permission.exception.editAnyDataSource", user.getPermissionHolderName()), user);
    }

    /**
     * Does this user have the global data source permission?
     * @param user
     * @return
     * @throws PermissionException
     */
    public boolean hasDataSourcePermission(PermissionHolder user) throws PermissionException {
        if (!isValidPermissionHolder(user)) return false;

        if(user.hasAdminRole()) return true;

        return hasPermission(user, dataSourcePermission.getPermission());
    }

    /**
     * Ensure the user can edit this data source.
     *  This method is more performant if you only have the data source ID as it
     *  won't need to lookup the entire data source to get the permission.
     * @param user
     * @param dataSourceId
     * @throws PermissionException
     */
    public void ensureDataSourceEditPermission(PermissionHolder user, int dataSourceId) throws PermissionException {
        if (!hasDataSourceEditPermission(user, dataSourceId)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.editDataSource", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Ensure the user can edit this data source
     * @param user
     * @param ds
     * @throws PermissionException
     */
    public void ensureDataSourceEditPermission(PermissionHolder user, DataSourceVO ds) throws PermissionException {
        if (!hasDataSourceEditPermission(user, ds))
            throw new PermissionException(new TranslatableMessage("permission.exception.editDataSource", user.getPermissionHolderName()), user);
    }

    /**
     * Does this permission holder have any of the edit roles on the data source?
     * @param user
     * @param ds
     * @return
     * @throws PermissionException
     */
    public boolean hasDataSourceEditPermission(PermissionHolder user, DataSourceVO ds) throws PermissionException {
        return hasPermission(user, ds.getEditPermission());
    }

    /**
     * Does this permission holder have any of the edit roles on the data source?
     *  This method is more performant if you only have the data source ID as it
     *  won't need to lookup the entire data source to get the permission.
     * @param user
     * @param dsId
     * @return
     * @throws PermissionException
     */
    public boolean hasDataSourceEditPermission(PermissionHolder user, int dsId) throws PermissionException {
        MangoPermission permission = roleDao.getPermission(dsId, DataSourceVO.class.getSimpleName(), EDIT);
        return hasPermission(user, permission);
    }

    /**
     * Ensure the user can read this data source.
     *  This method is more performant if you only have the data source ID as it
     *  won't need to lookup the entire data source to get the permission.
     * @param user
     * @param dataSourceId
     * @throws PermissionException
     */
    public void ensureDataSourceReadPermission(PermissionHolder user, int dataSourceId) throws PermissionException {
        if (!hasDataSourceReadPermission(user, dataSourceId)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.readDataSource", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Ensure the user can read this data source
     * @param user
     * @param ds
     * @throws PermissionException
     */
    public void ensureDataSourceReadPermission(PermissionHolder user, DataSourceVO ds) throws PermissionException {
        if (!hasDataSourceReadPermission(user, ds))
            throw new PermissionException(new TranslatableMessage("permission.exceptionreadDataSource", user.getPermissionHolderName()), user);
    }

    /**
     * Does this permission holder have any of the read roles on the data source?
     * @param user
     * @param ds
     * @return
     * @throws PermissionException
     */
    public boolean hasDataSourceReadPermission(PermissionHolder user, DataSourceVO ds) throws PermissionException {
        return hasPermission(user, ds.getReadPermission());
    }

    /**
     * Does this permission holder have any of the read roles on the data source?
     *  This method is more performant if you only have the data source ID as it
     *  won't need to lookup the entire data source to get the permission.
     * @param user
     * @param dsId
     * @return
     * @throws PermissionException
     */
    public boolean hasDataSourceReadPermission(PermissionHolder user, int dsId) throws PermissionException {
        MangoPermission permission = roleDao.getPermission(dsId, DataSourceVO.class.getSimpleName(), READ);
        return hasPermission(user, permission);
    }

    //
    //
    // Data point access
    //
    /**
     * Ensure the PermissionHolder can read the data point
     * @param user
     * @param point
     * @throws PermissionException
     */
    public void ensureDataPointReadPermission(PermissionHolder user, IDataPoint point) throws PermissionException {
        if (!hasDataPointReadPermission(user, point)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.readDataPoint", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Can this PermissionHolder read the data point?
     * @param user
     * @param point
     * @return
     * @throws PermissionException
     */
    public boolean hasDataPointReadPermission(PermissionHolder user, IDataPoint point) throws PermissionException {
        if (hasPermission(user, point.getReadPermission())) {
            return true;
        }else if(hasDataSourceReadPermission(user, point.getDataSourceId())) {
            return true;
        }else {
            return hasDataPointSetPermission(user, point);
        }
    }

    /**
     * Ensure this PermissionHolder set values on this data point.
     * @param user
     * @param point
     * @throws PermissionException
     */
    public void ensureDataPointSetPermission(PermissionHolder user, DataPointVO point) throws PermissionException {
        if (!hasDataPointSetPermission(user, point))
            throw new PermissionException(new TranslatableMessage("permission.exception.setDataPoint", user.getPermissionHolderName()), user);
    }

    /**
     * Can this PermissionHolder set values on this data point?
     * @param user
     * @param point
     * @return
     * @throws PermissionException
     */
    public boolean hasDataPointSetPermission(PermissionHolder user, IDataPoint point) throws PermissionException {
        if (hasPermission(user, point.getSetPermission())) {
            return true;
        }
        return hasDataSourceEditPermission(user, point.getDataSourceId());
    }

    /**
     * Does this holder have access to view this event type?
     * @param user
     * @param eventType
     * @return
     */
    public boolean hasEventTypePermission(PermissionHolder user, EventType eventType) {
        return hasAdminRole(user) || (hasEventsViewPermission(user) && eventType.hasPermission(user, this));
    }

    /**
     * Ensure this holder has access to view this event type
     * @param user
     * @param eventType
     * @throws PermissionException
     */
    public void ensureEventTypePermission(PermissionHolder user, EventType eventType) throws PermissionException {
        if (!hasEventTypePermission(user, eventType))
            throw new PermissionException(new TranslatableMessage("permission.exception.event", user.getPermissionHolderName()), user);
    }

    /**
     * Ensure this holder has access to view this event type VO
     * @param user
     * @param eventType
     * @throws PermissionException
     */
    public void ensureEventTypePermission(PermissionHolder user, EventTypeVO eventType) throws PermissionException {
        ensureEventTypePermission(user, eventType.getEventType());
    }

    /**
     * Can this user view any events?
     * @param user
     * @return
     */
    public boolean hasEventsViewPermission (PermissionHolder user) {
        if (!isValidPermissionHolder(user)) return false;

        if(user.hasAdminRole()) return true;

        return hasPermission(user, eventsViewPermission.getPermission());
    }

    /**
     * Ensure this user can view any events?
     * @param user
     */
    public void ensureEventsVewPermission(PermissionHolder user) {
        if (!hasEventsViewPermission(user))
            throw new PermissionException(new TranslatableMessage("permission.exception.event", user.getPermissionHolderName()), user);
    }

    /**
     * Does this permission holder have at least one of the required roles
     * @param user
     * @param requiredRoles
     * @return
     */
    public boolean hasAnyRole(PermissionHolder user, Set<Role> requiredRoles) {
        if (!isValidPermissionHolder(user)) return false;

        Set<Role> heldRoles = user.getRoles();
        return containsAnyRole(heldRoles, requiredRoles);
    }

    /**
     * Ensure this user has at least one of the roles
     * @param user
     * @param requiredPermissions
     */
    public void ensureHasAnyRole(PermissionHolder user, Set<Role> requiredRoles) {
        if (!hasAnyRole(user, requiredRoles)) {
            ensureValidPermissionHolder(user);
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Is the exact required role in the permission holder's roles?
     *  NOTE: superadmin must have this role for this to be true for them. i.e. they are not treated
     *  specially
     * @param user
     * @param requiredRole
     * @return
     */
    public boolean hasSingleRole(PermissionHolder user, Role requiredRole) {
        if (!isValidPermissionHolder(user)) return false;

        Set<Role> heldRoles = user.getRoles();
        return containsSingleRole(heldRoles, requiredRole);
    }

    /**
     *  Ensure the exact required role is in the permission holder's roles
     *  NOTE: superadmin must have this role for this to be true for them. i.e. they are not treated
     *  specially
     *
     * @param holder
     * @param requiredRole
     * @throws PermissionException
     */
    public void ensureSingleRole(PermissionHolder holder, Role requiredRole) throws PermissionException {
        if(!hasSingleRole(holder, requiredRole)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", holder.getPermissionHolderName()), holder);
        }
    }

    /**
     * Does this permission holder have all the required roles?
     * @param user
     * @param requiredRoles
     * @return
     */
    public boolean hasAllRoles(PermissionHolder user, Set<Role> requiredRoles) {
        if (!isValidPermissionHolder(user)) return false;

        Set<Role> heldRoles = user.getRoles();
        return containsAll(heldRoles, requiredRoles);
    }

    /**
     * Ensure this holder has all the required roles
     * @param user
     * @param requiredRoles
     */
    public void ensureHasAllRoles(PermissionHolder user, Set<Role> requiredRoles) {
        if (!hasAllRoles(user, requiredRoles)) {
            ensureValidPermissionHolder(user);
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Ensure this permission holder is valid
     * @param user
     */
    public void ensureValidPermissionHolder(PermissionHolder user)  throws PermissionException {
        if (user == null)
            throw new PermissionException(new TranslatableMessage("permission.exception.notAuthenticated"), null);
        if (user.isPermissionHolderDisabled())
            throw new PermissionException(new TranslatableMessage("permission.exception.userIsDisabled"), user);
    }

    /**
     * Is this permission holder valid, to be valid they:
     * - must be non null
     * - must not disabled
     *
     * @param user
     * @return
     */
    public boolean isValidPermissionHolder(PermissionHolder user) {
        return !(user == null || user.isPermissionHolderDisabled());
    }

    /**
     * Is the exact required role in the held roles?
     *  NOTE: superadmin will have to have this role for this to be true for them. i.e. they are not treated
     *  specially
     * @param heldRoles
     * @param requiredRole
     * @return
     */
    public boolean containsSingleRole(Set<Role> heldRoles, Role requiredRole) {
        // empty permissions string indicates that only superadmins are allowed access
        if (requiredRole == null) {
            return false;
        }

        return heldRoles.contains(requiredRole);
    }

    /**
     * Is every required role in the held roles?
     * @param heldRoles
     * @param requiredRoles
     * @return
     */
    private boolean containsAll(Set<Role> heldRoles, Set<Role> requiredRoles) {
        checkRoleSet(requiredRoles);

        if (heldRoles.contains(PermissionHolder.SUPERADMIN_ROLE)) {
            return true;
        }

        // empty permissions string indicates that only superadmins are allowed access
        if (requiredRoles.isEmpty()) {
            return false;
        }

        for(Role role : requiredRoles) {
            if(!heldRoles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is any required role in the held roles?
     * @param heldRoles
     * @param requiredRoles
     * @return
     */
    private boolean containsAnyRole(Set<Role> heldRoles, Set<Role> requiredRoles) {
        checkRoleSet(requiredRoles);
        //If I am superadmin or this has the default user role the we are good
        if (heldRoles.contains(PermissionHolder.SUPERADMIN_ROLE) || requiredRoles.contains(PermissionHolder.USER_ROLE)) {
            return true;
        }

        // empty roles indicates that only superadmins are allowed access
        if (requiredRoles.isEmpty()) {
            return false;
        }

        for (Role requiredRole : requiredRoles) {
            if (heldRoles.contains(requiredRole)) {
                return true;
            }
        }

        return false;
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
     * @param result - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param holder - the saving permission holder
     * @param savedByOwner - is the saving user the owner of this item (use false if no owner is possible)
     * @param existingPermission - the currently saved permissions
     * @param newPermission - the new permissions to validate
     */
    public void validateVoRoles(ProcessResult result, String contextKey, PermissionHolder holder,
            boolean savedByOwner, MangoPermission existingPermission, MangoPermission newPermission) {
        if (holder == null) {
            result.addContextualMessage(contextKey, "validate.userRequired");
            return;
        }

        if(newPermission == null) {
            result.addContextualMessage(contextKey, "validate.permission.null");
            return;
        }

        for (Set<Role> roles : newPermission.getRoles()) {
            if (roles == null) {
                result.addContextualMessage(contextKey, "validate.role.empty");
                return;
            } else {
                for(Role role : roles) {
                    if(role == null) {
                        result.addContextualMessage(contextKey, "validate.role.empty");
                    }else {
                        Integer id = roleDao.getIdByXid(role.getXid());
                        if( id == null) {
                            result.addContextualMessage(contextKey, "validate.role.notFound", role.getXid());
                        }else if (id != role.getId()) {
                            result.addContextualMessage(contextKey, "validate.role.invalidReference", role.getXid(), role.getId());
                        }
                    }
                }
            }
        }

        //Ensure no more than 64 role sets
        if(newPermission.getRoles().size() > 64) {
            result.addContextualMessage(contextKey, "validate.role.tooManyGroups");
            return;
        }

        if(holder.hasAdminRole())
            return;

        //Ensure the holder has at least one of the new permissions
        if(!savedByOwner && !newPermission.containsRole(PermissionHolder.USER_ROLE) && Collections.disjoint(holder.getRoles(), newPermission.getUniqueRoles())) {
            result.addContextualMessage(contextKey, "validate.mustRetainPermission");
        }

        if(existingPermission != null) {
            //Check for permissions being added that the user does not have
            Set<Role> added = new HashSet<>(newPermission.getUniqueRoles());
            added.removeAll(existingPermission.getUniqueRoles());
            added.removeAll(holder.getRoles());
            if(added.size() > 0) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", implodeRoles(holder.getRoles()));
            }
            //Check for permissions being removed that the user does not have
            Set<Role> removed = new HashSet<>(existingPermission.getUniqueRoles());
            removed.removeAll(newPermission.getUniqueRoles());
            removed.removeAll(holder.getRoles());
            if(removed.size() > 0) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", implodeRoles(holder.getRoles()));
            }
        }
        return;
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
    public UserRolesDetails getPermissionDetails(PermissionHolder currentUser, Collection<String> query, PermissionHolder user) {
        UserRolesDetails d = new UserRolesDetails(user.getPermissionHolderName(), user.hasAdminRole());

        boolean currentUserAdmin = currentUser.hasAdminRole();

        // Add any matching groups
        for (Role role : user.getRoles()) {
            if (currentUserAdmin || hasSingleRole(currentUser, role)) {
                d.getAllRoles().add(role);

                if (query.contains(role.getXid())) {
                    d.getMatchingRoles().add(role);
                }
            }
        }

        if (d.getAllRoles().size() == 0)
            return null;

        return d;
    }

    /**
     * Explode a legacy comma separated string into a set of Role objects
     *  if the Role doesn't exist it is still added but with an ID of -1
     * @param groups
     * @return
     */
    public Set<Role> explodeLegacyPermissionGroupsToRoles(String groups) {
        Set<String> permissions = explodeLegacyPermissionGroups(groups);
        return explodeLegacyPermissionGroupsToRoles(permissions);
    }

    /**
     * Explode a set of strings (role xids) into a set of Role objects
     *  if the Role doesn't exist it is still added but with an ID of -1
     * @param permissions
     * @return
     */
    public Set<Role> explodeLegacyPermissionGroupsToRoles(Set<String> permissions) {
        if(permissions == null) {
            return Collections.emptySet();
        }
        Set<Role> roles = new HashSet<>(permissions.size());
        for(String permission : permissions) {
            RoleVO vo = roleDao.getByXid(permission);
            if(vo != null) {
                roles.add(vo.getRole());
            }else {
                roles.add(new Role(Common.NEW_ID, permission));
            }
        }
        return roles;
    }

    /**
     * Check a role set so that
     *  - set cannot be null
     *  - role in set cannot be null
     *  - xid of role cannot be null
     * @param requiredPermissions
     */
    private static void checkRoleVOSet(Set<RoleVO> requiredRoles) {
        Objects.requireNonNull(requiredRoles, "Role set cannot be null");

        for (RoleVO requiredRole : requiredRoles) {
            if (requiredRole == null || requiredRole.getXid().isEmpty()) {
                throw new IllegalArgumentException("Role in set cannot be null or have empty role");
            }
        }
    }

    /**
     * Check a role set so that
     *  - set cannot be null
     *  - role in set cannot be null
     *  - xid of role cannot be null
     * @param requiredPermissions
     */
    private static void checkRoleSet(Set<Role> requiredRoles) {
        Objects.requireNonNull(requiredRoles, "Role set cannot be null");

        for (Role requiredRole : requiredRoles) {
            if (requiredRole == null || requiredRole.getXid().isEmpty()) {
                throw new IllegalArgumentException("Role in set cannot be null or have empty role");
            }
        }
    }

    /**
     * Turn a set of RoleVOs into a comma separated list for display in a message
     * @param roles
     * @return
     */
    public static String implodeRoleVOs(Set<RoleVO> roles) {
        checkRoleVOSet(roles);
        return String.join(",", roles.stream().map(role -> role.getXid()).collect(Collectors.toSet()));
    }

    /**
     * Turn a set of roles into a comma separated list for display in a message
     * @param roles
     * @return
     */
    public static String implodeRoles(Set<Role> roles) {
        if(roles == null)
            return "";
        return String.join(",", roles.stream().map(role -> role.getXid()).collect(Collectors.toSet()));
    }

    /**
     * Explode a comma separated group of permissions (roles) from the legacy format
     * @param groups
     * @return
     */
    public static Set<String> explodeLegacyPermissionGroups(String groups) {
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
     * This should only be called on the upgrade to Mango 4.0 as it will create new roles,
     *  it is designed to be used during serialization to extract and create roles from serialized data
     * @param permissions
     * @return
     */
    public static MangoPermission upgradePermissions(String permissions) {
        Set<String> permissionSet = PermissionService.explodeLegacyPermissionGroups(permissions);
        return upgradePermissions(permissionSet);
    }

    /**
     * This should only be called on the upgrade to Mango 4.0 as it will create new roles,
     *  it is designed to be used during serialization to extract and create roles from serialized data
     * @param permissionSet
     * @return
     */
    public static MangoPermission upgradePermissions(Set<String> permissionSet) {
        if(permissionSet == null) {
            return new MangoPermission();
        }

        Set<Role> roles = new HashSet<>();
        for(String permission : permissionSet) {
            RoleVO role = RoleDao.getInstance().getByXid(permission);
            if(role != null) {
                roles.add(role.getRole());
            }else {
                RoleVO r = new RoleVO(Common.NEW_ID, permission, permission);
                try {
                    RoleDao.getInstance().insert(r);
                    roles.add(r.getRole());
                }catch(Exception e) {
                    //Someone maybe inserted this role while we were doing this.
                    roles.add(r.getRole());
                }
            }
        }

        return MangoPermission.createOrSet(roles);
    }
}
