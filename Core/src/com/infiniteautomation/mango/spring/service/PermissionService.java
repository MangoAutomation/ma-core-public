/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
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
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.permission.UserRolesDetails;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DataSourcePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventsViewPermissionDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
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

    private final RoleDao roleDao;
    private final DataSourcePermissionDefinition dataSourcePermission;
    private final PermissionHolder systemSuperadmin;
    private final EventsViewPermissionDefinition eventsViewPermission;

    //Cache of role xid to inheritance
    private final Cache<String, RoleInheritance> roleHierarchyCache;

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
        this.roleHierarchyCache = Caffeine.newBuilder().maximumSize(Common.envProps.getLong("permissions.roles.inheritanceCacheSize", 1000)).build();
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
            SecurityContextHolder.setContext(newSecurityContext(user));
            command.run();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    public <T> T runAs(PermissionHolder user, Supplier<T> command) {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(newSecurityContext(user));
            return command.get();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    public <T> T runAsCallable(PermissionHolder user, Callable<T> command) throws Exception {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(newSecurityContext(user));
            return command.call();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    /**
     * Creates a proxy object for the supplied instance where every method invoked is run as the supplied user.
     * The returned proxy object will implement all interfaces of the supplied instance.
     *
     * @param <T> must be an interface
     * @param user
     * @param instance
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T runAsProxy(PermissionHolder user, T instance) {
        Class<?> clazz = instance.getClass();
        SecurityContext runAsContext = newSecurityContext(user);

        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), (proxy, method, args) -> {
            SecurityContext original = SecurityContextHolder.getContext();
            try {
                SecurityContextHolder.setContext(runAsContext);
                return method.invoke(instance, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } finally {
                SecurityContextHolder.setContext(original);
            }
        });
    }

    /**
     * Creates a new security context for the supplied user
     * @param user
     * @return
     */
    private SecurityContext newSecurityContext(PermissionHolder user) {
        SecurityContext newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(new PreAuthenticatedAuthenticationToken(user, null));
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
                if(hasAdminRole(holder) || hasPermission(holder, def.getValue().getPermission())) {
                    grantedPermissions.add(def.getValue().getPermissionTypeName());
                }
            }
        }
        return grantedPermissions;
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

        if(hasAdminRole(user)) return true;

        return hasPermission(user, dataSourcePermission.getPermission());
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

        if(hasAdminRole(user)) return true;

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

        Set<Role> heldRoles = getAllInheritedRoles(user);
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

        Set<Role> heldRoles = getAllInheritedRoles(user);
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
     * Does this permission holder have all the required roles?  This will check
     *  the inherited roles too.
     * @param user
     * @param requiredRoles
     * @return
     */
    public boolean hasAllRoles(PermissionHolder user, Set<Role> requiredRoles) {
        if (!isValidPermissionHolder(user)) return false;

        Set<Role> heldRoles = getAllInheritedRoles(user);
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
     * Get a cached role, for performance
     *
     * Using this method will fill an entry in the cache and compute the
     *  inheritance, assuming it will be used at some point later
     * @param roleXid
     * @return null if role does not exist
     */
    public Role getRole(String roleXid) {
        RoleInheritance inheritance = roleHierarchyCache.get(roleXid, (xid) -> {
            RoleInheritance i = new RoleInheritance();
            RoleVO roleVo = roleDao.getByXid(xid);
            if(roleVo == null) {
                return null;
            }
            i.role = roleVo.getRole();
            i.inherited = roleDao.getFlatInheritance(roleVo.getRole());
            i.inheritedBy = roleDao.getRolesThatInherit(roleVo.getRole());
            return i;
        });
        if(inheritance == null) {
            return null;
        }else {
            return inheritance.role;
        }
    }

    /**
     * Get a set of this role and all roles that inherit this role
     * @param roleXid
     * @return
     */
    public Set<Role> getRolesThatInherit(String roleXid) {
        Set<Role> allRoles = new HashSet<>();

        RoleInheritance inheritance = roleHierarchyCache.get(roleXid, (xid) -> {
            RoleInheritance i = new RoleInheritance();
            RoleVO roleVo = roleDao.getByXid(xid);
            if(roleVo == null) {
                return null;
            }
            i.role = roleVo.getRole();
            i.inherited = roleDao.getFlatInheritance(roleVo.getRole());
            i.inheritedBy = roleDao.getRolesThatInherit(roleVo.getRole());
            return i;
        });

        if(inheritance!= null) {
            allRoles.add(inheritance.role);
            allRoles.addAll(inheritance.inheritedBy);
        }
        return Collections.unmodifiableSet(allRoles);
    }

    /**
     * Get the permission holders roles and all roles inherited by those roles
     * @param holder
     * @return
     */
    public Set<Role> getAllInheritedRoles(PermissionHolder holder) {
        Set<Role> allRoles = new HashSet<>();
        for(Role role : holder.getRoles()) {
            RoleInheritance inheritance = roleHierarchyCache.get(role.getXid(), (xid) -> {
                RoleInheritance i = new RoleInheritance();
                i.role = role;
                i.inherited = roleDao.getFlatInheritance(role);
                i.inheritedBy = roleDao.getRolesThatInherit(role);
                return i;
            });
            if(inheritance != null) {
                allRoles.add(role);
                allRoles.addAll(inheritance.inherited);
            }
        }
        return Collections.unmodifiableSet(allRoles);
    }

    /**
     * Validate a permission.  This will validate that:
     *
     *   1. the new roles are non null
     *   2. all new roles are not empty
     *   3. the new roles do exist
     *   (then for non admin/owners)
     *   4. the saving user will at least retain permission
     *   5. the user cannot not remove an existing role they do not have
     *   6. the user has all of the new roles being added
     *
     *   If the saving user is also the owner, then the new roles need not contain
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

        if(hasAdminRole(holder))
            return;

        Set<Role> inherited = getAllInheritedRoles(holder);

        //Ensure the holder has at least one of the new permissions
        if(!savedByOwner && !newPermission.containsRole(PermissionHolder.USER_ROLE) && Collections.disjoint(inherited, newPermission.getUniqueRoles())) {
            result.addContextualMessage(contextKey, "validate.mustRetainPermission");
        }

        if(existingPermission != null) {
            //Check for permissions being added that the user does not have
            Set<Role> added = new HashSet<>(newPermission.getUniqueRoles());
            added.removeAll(existingPermission.getUniqueRoles());
            added.removeAll(inherited);
            if(added.size() > 0) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", implodeRoles(inherited));
            }
            //Check for permissions being removed that the user does not have
            Set<Role> removed = new HashSet<>(existingPermission.getUniqueRoles());
            removed.removeAll(newPermission.getUniqueRoles());
            removed.removeAll(inherited);
            if(removed.size() > 0) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", implodeRoles(inherited));
            }
        }
        return;
    }

    /**
     * Validate roles.  Used for things like a User or ScriptPermission.
     *   This will validate that:
     *
     *   1. the new roles are non null
     *   2. all new roles are not empty
     *   3. the new roles exist
     *   (then for non admin/owners)
     *   4. the user cannot not remove an existing role they do not have
     *   5. the user has all of the new roles being added
     *
     *
     * @param result - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param holder - the saving permission holder
     * @param savedByOwner - is the saving user the owner of this item (use false if no owner is possible)
     * @param existingRoles - the currently saved permissions
     * @param newRoles - the new permissions to validate
     */
    public void validatePermissionHolderRoles(ProcessResult result, String contextKey, PermissionHolder holder,
            boolean savedByOwner, Set<Role> existingRoles, Set<Role> newRoles) {
        if (holder == null) {
            result.addContextualMessage(contextKey, "validate.userRequired");
            return;
        }

        if(newRoles == null) {
            result.addContextualMessage(contextKey, "validate.permission.null");
            return;
        }

        for (Role role : newRoles) {
            if (role == null) {
                result.addContextualMessage(contextKey, "validate.role.empty");
                return;
            } else {
                Integer id = roleDao.getIdByXid(role.getXid());
                if( id == null) {
                    result.addContextualMessage(contextKey, "validate.role.notFound", role.getXid());
                }else if (id != role.getId()) {
                    result.addContextualMessage(contextKey, "validate.role.invalidReference", role.getXid(), role.getId());
                }
            }
        }

        if(hasAdminRole(holder))
            return;

        Set<Role> inherited = getAllInheritedRoles(holder);

        //Ensure the holder has at least one of the new permissions
        if(!savedByOwner && !newRoles.contains(PermissionHolder.USER_ROLE) && Collections.disjoint(inherited, newRoles)) {
            result.addContextualMessage(contextKey, "validate.mustRetainPermission");
        }

        if(existingRoles != null) {
            //Check for permissions being added that the user does not have
            Set<Role> added = new HashSet<>(newRoles);
            added.removeAll(existingRoles);
            added.removeAll(inherited);
            if(added.size() > 0) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", PermissionService.implodeRoles(inherited));
            }
            //Check for permissions being removed that the user does not have
            Set<Role> removed = new HashSet<>(existingRoles);
            removed.removeAll(newRoles);
            removed.removeAll(inherited);
            if(removed.size() > 0) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", PermissionService.implodeRoles(inherited));
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
        UserRolesDetails d = new UserRolesDetails(user.getPermissionHolderName(), hasAdminRole(user));

        boolean currentUserAdmin = hasAdminRole(currentUser);

        // Add any matching groups
        Set<Role> inherited = getAllInheritedRoles(user);
        for (Role role : inherited) {
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
            RoleVO vo = permission != null ? roleDao.getByXid(permission) : null;
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
     * Keep our cache up to date by evicting changed roles
     * @param event
     */
    @EventListener
    protected void handleRoleEvent(DaoEvent<? extends RoleVO> event) {
        switch(event.getType()) {
            case DELETE:
            case UPDATE:
                //TODO Mango 4.0 Invalidate all inherited roles
                //TODO Mango 4.0 Invalidate all roles that inherit
                //TODO Invalidate me
                roleHierarchyCache.invalidateAll();
                break;
            default:
                break;
        }
    }

    /**
     * This should only be called on the upgrade to Mango 4.0 as it will create new roles,
     *  it is designed to be used during serialization to extract and create roles from serialized data
     * @param permissionSet
     * @return
     */
    public Set<Role> upgradeScriptRoles(Set<String> permissionSet) {
        if(permissionSet == null) {
            return new HashSet<>();
        }

        Set<Role> roles = new HashSet<>();
        for(String permission : permissionSet) {
            RoleVO role = roleDao.getByXid(permission);
            if(role != null) {
                roles.add(role.getRole());
            }else {
                RoleVO r = new RoleVO(Common.NEW_ID, permission, permission);
                try {
                    roleDao.insert(r);
                    roles.add(r.getRole());
                }catch(Exception e) {
                    //Someone maybe inserted this role while we were doing this.
                    roles.add(r.getRole());
                }
            }
        }

        return roles;
    }

    private static final class RoleInheritance {
        Role role;
        Set<Role> inherited;
        Set<Role> inheritedBy;
    }
}
