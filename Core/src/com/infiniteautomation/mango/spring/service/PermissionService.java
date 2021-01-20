/*
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.infiniteautomation.mango.cache.BidirectionalCache;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.util.Functions;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PermissionDao;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DataSourcePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventsViewPermissionDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.permission.OwnedResource;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
@Service
public class PermissionService implements CachingService {

    private final RoleDao roleDao;
    private final PermissionDao permissionDao;

    private final DataSourcePermissionDefinition dataSourcePermission;
    private final PermissionHolder systemSuperadmin;
    private final EventsViewPermissionDefinition eventsViewPermission;

    //Cache of role xid to inheritance
    private final LoadingCache<String, RoleInheritance> roleHierarchyCache;
    //Cache of permissionId to MangoPermission

    private final BidirectionalCache<Integer, MangoPermission> permissionCache;
    private final BidirectionalCache<MangoPermission, Integer> permissionCacheInverse;

    @Autowired
    public PermissionService(RoleDao roleDao,
            PermissionDao permissionDao,
            @Qualifier(MangoRuntimeContextConfiguration.SYSTEM_SUPERADMIN_PERMISSION_HOLDER)
    PermissionHolder systemSuperadmin,
    DataSourcePermissionDefinition dataSourcePermission,
    EventsViewPermissionDefinition eventsView,
                             Environment env) {
        this.roleDao = roleDao;
        this.permissionDao = permissionDao;

        this.dataSourcePermission = dataSourcePermission;
        this.systemSuperadmin = systemSuperadmin;
        this.eventsViewPermission = eventsView;
        this.roleHierarchyCache = Caffeine.newBuilder()
                .maximumSize(env.getProperty("cache.roles.size", Long.class, 1000L))
                .build(this::loadRoleInheritance);
        this.permissionCache = new BidirectionalCache<>(env.getProperty("cache.permission.size", Integer.class, 1000));
        this.permissionCacheInverse = this.permissionCache.inverse();
    }

    /**
     * Does this user have the superadmin role?
     * @param user the user to test
     * @return
     */
    public boolean hasAdminRole(PermissionHolder user) {
        if (!isValidPermissionHolder(user)) return false;
        Set<Role> heldRoles = getAllInheritedRoles(user);
        return heldRoles.contains(PermissionHolder.SUPERADMIN_ROLE);
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
     * Can be used to check if the user has the "user" role i.e. the permission holder is not anonymous
     * @param user the user to check
     * @return true if user has the "user" role
     */
    public boolean hasUserRole(PermissionHolder user) {
        if (!isValidPermissionHolder(user)) return false;
        Set<Role> heldRoles = getAllInheritedRoles(user);
        return heldRoles.contains(PermissionHolder.USER_ROLE);
    }

    /**
     * Can be used to ensure the user has the "user" role i.e. the permission holder is not anonymous
     * @param user the user to check
     * @throws PermissionException if user does not have the "user" role
     */
    public void ensureUserRole(PermissionHolder user) throws PermissionException {
        if (!hasUserRole(user)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Does this permission holder have access based on the permission's role logic?
     * @param user
     * @param permission
     * @return
     */
    public boolean hasPermission(PermissionHolder user, MangoPermission permission) {
        Objects.requireNonNull(permission);
        if (!isValidPermissionHolder(user)) return false;

        Set<Role> inherited = getAllInheritedRoles(user);
        if (inherited.contains(PermissionHolder.SUPERADMIN_ROLE)) {
            return true;
        }

        Set<Set<Role>> minterms = permission.getRoles();
        return minterms.stream().anyMatch(inherited::containsAll);
    }

    /**
     * Ensure the user has a role defined in this permission
     * @param user
     * @param permission
     */
    public void ensurePermission(PermissionHolder user, MangoPermission permission) {
        if (!hasPermission(user, permission)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Checks that the user has all of the roles that are assigned to the other PermissionHolder.
     *
     * @param user the current user
     * @param other other
     * @return true if user has all the roles of other
     */
    public boolean hasSupersetOfRoles(PermissionHolder user, PermissionHolder other) {
        if (!isValidPermissionHolder(user)) return false;
        Set<Role> heldRoles = getAllInheritedRoles(user);
        return heldRoles.containsAll(other.getRoles());
    }

    /**
     * Ensures that the user has all of the roles that are assigned to the other PermissionHolder.
     *
     * @param user the current user
     * @param other other
     */
    public void ensureSupersetOfRoles(PermissionHolder user, PermissionHolder other) {
        if (!hasSupersetOfRoles(user, other)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
        }
    }

    /**
     * Return all the granted system permissions a user has.  This is any PermissionDefinition that the user
     * has permission for.
     *
     * THIS SHOULD NOT BE USED FOR AUTHORIZATION / ACCESS CONTROL.
     *
     * @param holder
     * @return
     */
    public Set<String> getSystemPermissions(PermissionHolder holder) {
        Set<String> systemPermissions = new HashSet<>();
        for (PermissionDefinition def : ModuleRegistry.getPermissionDefinitions().values()) {
            if (hasPermission(holder, def.getPermission())) {
                systemPermissions.add(def.getPermissionTypeName());
            }
        }
        return systemPermissions;
    }

    /**
     * TODO Mango 4.0 remove method
     *
     * Ensure this user have the global data source permission
     * @param user
     * @throws PermissionException
     */
    public void ensureDataSourcePermission(PermissionHolder user) throws PermissionException {
        if (!hasDataSourcePermission(user))
            throw new PermissionException(new TranslatableMessage("permission.exception.editAnyDataSource", user.getPermissionHolderName()), user);
    }

    /**
     * TODO Mango 4.0 remove method
     *
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
     * TODO Mango 4.0 remove method
     *
     * Does this holder have access to view this event type?
     * @param user
     * @param eventType
     * @return
     */
    public boolean hasEventTypePermission(PermissionHolder user, EventType eventType) {
        return hasAdminRole(user) || (hasEventsViewPermission(user) && eventType.hasPermission(user, this));
    }

    /**
     * TODO Mango 4.0 remove method
     *
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
     * TODO Mango 4.0 remove method
     *
     * Ensure this holder has access to view this event type VO
     * @param user
     * @param eventType
     * @throws PermissionException
     */
    public void ensureEventTypePermission(PermissionHolder user, EventTypeVO eventType) throws PermissionException {
        ensureEventTypePermission(user, eventType.getEventType());
    }

    /**
     * TODO Mango 4.0 remove method
     *
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
     * TODO Mango 4.0 remove method
     *
     * Ensure this user can view any events?
     * @param user
     */
    public void ensureEventsVewPermission(PermissionHolder user) {
        if (!hasEventsViewPermission(user))
            throw new PermissionException(new TranslatableMessage("permission.exception.event", user.getPermissionHolderName()), user);
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
     * Ensure this PermissionHolder has read permission for this point
     * @param user
     * @param dataPointId
     * @throws PermissionException
     */
    public void ensureDataPointReadPermission(PermissionHolder user, int dataPointId) throws PermissionException {
        if (!hasDataPointReadPermission(user, dataPointId))
            throw new PermissionException(new TranslatableMessage("permission.exception.readDataPoint", user.getPermissionHolderName()), user);
    }

    /**
     * Does this PermissionHolder have read permission for this point
     * @param user
     * @param dataPointId
     * @return
     */
    public boolean hasDataPointReadPermission(PermissionHolder user, int dataPointId) {
        if (!isValidPermissionHolder(user)) return false;

        //TODO Mango 4.0 after removing the getInstance() method we can use @Lazy to inject this dao
        Integer permissionId = DataPointDao.getInstance().getReadPermissionId(dataPointId);
        if(permissionId == null) {
            return hasAdminRole(user);
        }else {
            MangoPermission permission = permissionCache.computeIfAbsent(permissionId, this::loadPermission);
            return hasPermission(user, permission);
        }
    }

    /**
     * Does this PermissionHolder have read permission for this source
     * @param user
     * @param dataSourceId
     * @return
     */
    public boolean hasDataSourceReadPermission(PermissionHolder user, int dataSourceId) {
        if (!isValidPermissionHolder(user)) return false;

        //TODO Mango 4.0 after removing the getInstance() method we can use @Lazy to inject this dao
        Integer permissionId = DataSourceDao.getInstance().getReadPermissionId(dataSourceId);
        if(permissionId == null) {
            return hasAdminRole(user);
        }else {
            MangoPermission permission = permissionCache.computeIfAbsent(permissionId, this::loadPermission);
            return hasPermission(user, permission);
        }
    }

    /**
     * Ensure this PermissionHolder has read permission for this source
     * @param user
     * @param dataSourceId
     * @throws PermissionException
     */
    public void ensureDataSourceReadPermission(PermissionHolder user, int dataSourceId) throws PermissionException {
        if (!hasDataSourceReadPermission(user, dataSourceId))
            throw new PermissionException(new TranslatableMessage("permission.exception.readDataSource", user.getPermissionHolderName()), user);
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
     * Get a cached role, for performance
     *
     * Using this method will fill an entry in the cache and compute the
     *  inheritance, assuming it will be used at some point later
     * @param roleXid
     * @return null if role does not exist
     */
    public Role getRole(String roleXid) {
        RoleInheritance inheritance = roleHierarchyCache.get(roleXid);
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

        RoleInheritance inheritance = roleHierarchyCache.get(roleXid);

        if(inheritance!= null) {
            allRoles.add(inheritance.role);
            allRoles.addAll(inheritance.inheritedBy);
        }
        return Collections.unmodifiableSet(allRoles);
    }

    private RoleInheritance loadRoleInheritance(String xid) {
        RoleInheritance i = new RoleInheritance();
        RoleVO roleVo = roleDao.getByXid(xid);
        if(roleVo == null) {
            return null;
        }
        i.role = roleVo.getRole();
        i.inherited = roleDao.getFlatInheritance(roleVo.getRole());
        i.inheritedBy = roleDao.getRolesThatInherit(roleVo.getRole());
        return i;
    }

    /**
     * Get the permission holders roles and all roles inherited by those roles
     * @param holder
     * @return
     */
    public Set<Role> getAllInheritedRoles(PermissionHolder holder) {
        Set<Role> allRoles = new HashSet<>(holder.getRoles());
        for (Role role : holder.getRoles()) {
            RoleInheritance inheritance = roleHierarchyCache.get(role.getXid());
            if (inheritance != null) {
                allRoles.addAll(inheritance.inherited);
            }
        }
        return Collections.unmodifiableSet(allRoles);
    }

    /**
     * Get a permission from the cache, load from db if necessary
     * @param id
     * @return
     * @throws NotFoundException if permission with this ID not found
     */
    public MangoPermission get(Integer id) throws NotFoundException {
        Objects.requireNonNull(id);
        MangoPermission permission = permissionCache.computeIfAbsent(id, this::loadPermission);
        if (permission == null) {
            throw new NotFoundException();
        } else {
            return permission.withId(id);
        }
    }

    /**
     * Finds or creates a saved permission with minterms that match the desired permission.
     * If the permission already has a non-null id it is assumed to have been saved already and is returned directly.
     * @param permission
     * @return saved permission with an ID
     */
    public MangoPermission findOrCreate(MangoPermission permission) {
        if (permission.getId() == null) {
            Integer id = permissionCacheInverse.computeIfAbsent(permission, r -> permissionDao.permissionId(r.getRoles()));
            return permission.withId(id);
        }
        return permission;
    }

    /**
     * Attempt to delete permissions, if other VOs reference the permission it will not be deleted
     *
     * @param permissions
     */
    public void deletePermissions(MangoPermission... permissions) {
        for (MangoPermission permission : permissions) {
            this.deletePermissionId(permission.getId());
        }
    }

    /**
     * Attempt to delete permission from system by ID, if other VOs reference this permission it will not be deleted
     *
     * @param permissionId
     */
    public void deletePermissionId(Integer permissionId) {
        permissionCache.compute(permissionId, (id, perm) -> {
            if (permissionDao.deletePermission(id)) {
                // remove cache entry if it exists
                return null;
            }
            // keep cache entry if it exists
            return perm;
        });
    }

    /**
     * Load a permission from the database via it's id, used by cache
     * @param id
     * @return
     */
    private MangoPermission loadPermission(Integer id) {
        return permissionDao.get(id);
    }

    /**
     * Validate a permission.  This will validate that:
     *
     *   1. the new roles are non null
     *   2. all new roles are not empty
     *   3. the new roles do exist
     *   4. the saving user will at least retain permission
     *
     *   If the saving user is the owner then the user does not need to have access to the permission.
     *
     * @param result - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param holder - the saving permission holder
     * @param existingPermission - the currently saved permissions
     * @param newPermission - the new permissions to validate
     */
    public void validatePermission(ProcessResult result, String contextKey, PermissionHolder holder,
            MangoPermission existingPermission, MangoPermission newPermission) {

        if (holder == null) {
            result.addContextualMessage(contextKey, "validate.userRequired");
            return;
        }

        if (newPermission == null) {
            result.addContextualMessage(contextKey, "validate.permission.null");
            return;
        }

        for (Set<Role> roles : newPermission.getRoles()) {
            validateRoles(result, contextKey, roles);
        }
        if (!result.isValid()) {
            return;
        }

        // Ensure the user retains access to the object
        if (!hasPermission(holder, newPermission)) {
            result.addContextualMessage(contextKey, "validate.mustRetainPermission");
        }
    }

    /**
     * Validate roles.  Used for things like a User or ScriptPermission.
     *   This will validate that:
     *
     *   1. the new roles are non null
     *   2. all new roles are not empty
     *   3. the new roles exist
     *   4. the user cannot not modify their own roles
     *   5. the user cannot assign a role they do not have
     *
     * @param result - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param holder - the saving permission holder
     * @param existingRoles - the currently saved permissions
     * @param newRoles - the new permissions to validate
     */
    public void validatePermissionHolderRoles(ProcessResult result, String contextKey,
            PermissionHolder holder, Set<Role> existingRoles, Set<Role> newRoles) {

        if (holder == null) {
            result.addContextualMessage(contextKey, "validate.userRequired");
            return;
        }

        if (newRoles == null) {
            result.addContextualMessage(contextKey, "validate.permission.null");
            return;
        }

        validateRoles(result, contextKey, newRoles);
        if (!result.isValid()) {
            return;
        }

        Set<Role> heldRoles = holder.getRoles();
        if (!heldRoles.contains(PermissionHolder.SUPERADMIN_ROLE)) {
            Set<Role> inherited = getAllInheritedRoles(holder);
            if (!inherited.containsAll(newRoles)) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", implodeRoles(inherited));
            }
        }
    }

    private void validateRoles(ProcessResult result, String contextKey, Set<Role> roles) {
        for (Role role : roles) {
            Integer id = roleDao.getIdByXid(role.getXid());
            if (id == null) {
                result.addContextualMessage(contextKey, "validate.role.notFound", role.getXid());
            } else if (id != role.getId()) {
                result.addContextualMessage(contextKey, "validate.role.invalidReference", role.getXid(), role.getId());
            }
        }
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
     * TODO Mango 4.0 remove
     * Turn a set of RoleVOs into a comma separated list for display in a message
     * @param roles
     * @return
     */
    public static String implodeRoleVOs(Set<RoleVO> roles) {
        return String.join(",", roles.stream().map(AbstractVO::getXid).collect(Collectors.toSet()));
    }

    /**
     * TODO Mango 4.0 remove
     * Turn a set of roles into a comma separated list for display in a message
     * @param roles
     * @return
     */
    public static String implodeRoles(Set<Role> roles) {
        if(roles == null)
            return "";
        return String.join(",", roles.stream().map(Role::getXid).collect(Collectors.toSet()));
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
            s = s.replaceAll(Functions.WHITESPACE_REGEX, "");
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
                //TODO Mango 4.0 find and invalidate permissions that have this role
                permissionCache.clear();
                break;
            default:
                break;
        }
    }

    @Override
    public void clearCaches() {
        PermissionHolder currentUser = Common.getUser();
        ensureAdminRole(currentUser);
        this.roleHierarchyCache.invalidateAll();
        this.permissionCache.clear();
    }

    /**
     * TODO Mango 4.0 remove
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

    /**
     * Does this user have access to owned resource
     * @param user the user to test
     * @param resource the owned resource
     * @return true if user has access to resource.
     */
    public boolean hasAccessToResource(PermissionHolder user, OwnedResource resource) {
        return isValidPermissionHolder(user) && (resource.isOwnedBy(user) || hasAdminRole(user));
    }

    /**
     * Ensure user has access to owned resource
     *
     * @param holder
     */
    public void ensureAccessToResource(PermissionHolder holder, OwnedResource resource) throws PermissionException {
        if (!hasAccessToResource(holder, resource)) {
            throw new PermissionException(new TranslatableMessage("permission.exception.userDoesNotOwnResource", holder.getPermissionHolderName()), holder);
        }
    }

    private static final class RoleInheritance {
        Role role;
        Set<Role> inherited;
        Set<Role> inheritedBy;
    }
}
