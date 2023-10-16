/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.infiniteautomation.mango.cache.BidirectionalCache;
import com.infiniteautomation.mango.permission.MangoPermission;
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
import com.serotonin.m2m2.module.definitions.permissions.EventsSuperadminViewPermissionDefinition;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.permission.OwnedResource;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 *
 */
@SuppressWarnings("JavaDoc")
@Service
public class PermissionService implements CachingService {

    private final RoleDao roleDao;
    private final PermissionDao permissionDao;

    private final EventsSuperadminViewPermissionDefinition eventsSuperadminViewPermissionDefinition;

    //Cache of role xid to inheritance
    private final LoadingCache<String, RoleInheritance> roleHierarchyCache;
    //Cache of permissionId to MangoPermission

    private final BidirectionalCache<Integer, MangoPermission> permissionCache;
    private final BidirectionalCache<MangoPermission, Integer> permissionCacheInverse;

    @Autowired
    public PermissionService(RoleDao roleDao,
                             PermissionDao permissionDao,
                             @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EventsSuperadminViewPermissionDefinition eventsSuperadminViewPermissionDefinition,
                             Environment env) {
        this.roleDao = roleDao;
        this.permissionDao = permissionDao;
        this.eventsSuperadminViewPermissionDefinition = eventsSuperadminViewPermissionDefinition;
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
     * Get a cached role, for performance. Using this method will fill an entry in the cache and compute the
     * inheritance, assuming it will be used at some point later.
     *
     * @param roleXid
     * @return null if role does not exist
     */
    public @Nullable Role getRole(String roleXid) {
        RoleInheritance inheritance = roleHierarchyCache.get(roleXid);
        if(inheritance == null) {
            return null;
        }else {
            return inheritance.role;
        }
    }

    public Set<Role> splitMinterm(String minterm) {
        return Arrays.stream(minterm.split(","))
                .map(xid -> {
                    Role role = getRole(xid);
                    if (role == null) {
                        throw new IllegalArgumentException("Role not found");
                    }
                    return role;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Load roles by their XIDs and returns a new permission without any minterms that contained a missing role.
     * Used when deserializing a permission from a data column, or when receiving a role from a foreign source.
     * The returned permission is unsaved and will have an id of -1.
     *
     * @param permission a permission with roles that potentially don't exist
     * @return an unsaved permission with roles loaded
     */
    public MangoPermission loadRoles(MangoPermission permission) {
        Set<Set<Role>> minterms = permission.getRoles().stream()
                .map(mt -> mt.stream()
                        .map(r -> getRole(r.getXid()))
                        .collect(Collectors.toSet()))
                .filter(mt -> !mt.contains(null) && !mt.isEmpty())
                .collect(Collectors.toSet());
        return new MangoPermission(minterms);
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
     * Get a set of this role and all roles inherited by it
     * @param role
     * @return
     */
    public Set<Role> getAllInheritedRoles(Role role) {
        Set<Role> allRoles = new HashSet<>();
        allRoles.add(role);
        RoleInheritance inheritance = roleHierarchyCache.get(role.getXid());
        if (inheritance != null) {
            allRoles.addAll(inheritance.inherited);
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
     * See {@link #validatePermission(ProcessResult, String, PermissionHolder, MangoPermission, MangoPermission, boolean)}
     */
    public void validatePermission(ProcessResult result, String contextKey, PermissionHolder holder,
                                   @Nullable MangoPermission existingPermission, MangoPermission newPermission) {
        validatePermission(result, contextKey, holder, existingPermission, newPermission, true);
    }

    /**
     * See {@link #validatePermission(ProcessResult, String, PermissionHolder, MangoPermission, MangoPermission, boolean)}
     */
    public void validatePermission(ProcessResult result, String contextKey, PermissionHolder holder,
                                   MangoPermission newPermission) {
        validatePermission(result, contextKey, holder, null, newPermission, true);
    }

    /**
     * See {@link #validatePermission(ProcessResult, String, PermissionHolder, MangoPermission, MangoPermission, boolean)}
     */
    public void validatePermission(ProcessResult result, String contextKey, PermissionHolder holder,
                                   MangoPermission newPermission,
                                   boolean mustRetainAccess) {
        validatePermission(result, contextKey, holder, null, newPermission, mustRetainAccess);
    }

    /**
     * Validate a permission.  This will validate that:
     * <ol>
     *     <li>The roles are non null</li>
     *     <li>All roles are not empty</li>
     *     <li>The roles exist</li>
     *     <li>The current user cannot add the user or anonymous role unless they have the superadmin role</li>
     *     <li>The current user cannot add any roles that they do not hold</li>
     *     <li>The current user will retain access to this permission (if mustRetainAccess is true)</li>
     * </ol>
     * @param result - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param holder - the saving permission holder
     * @param existingPermission - the existing permission (may be null)
     * @param newPermission - the new permissions to validate
     * @param mustRetainAccess set to true if the current user must retain access
     */
    public void validatePermission(ProcessResult result, String contextKey, PermissionHolder holder,
                                   @Nullable MangoPermission existingPermission, MangoPermission newPermission,
                                   boolean mustRetainAccess) {

        Assert.notNull(result, "result must not be null");
        Assert.notNull(contextKey, "contextKey must not be null");
        Assert.notNull(holder, "holder must not be null");

        if (newPermission == null) {
            result.addContextualMessage(contextKey, "validate.permission.null");
            return;
        }

        boolean rolesValid = true;
        for (Set<Role> roles : newPermission.getRoles()) {
            rolesValid = validateRoles(result, contextKey, roles) && rolesValid;
        }

        if (rolesValid && mustRetainAccess) {
            // Ensure the user retains access to the object
            if (!hasPermission(holder, newPermission)) {
                result.addContextualMessage(contextKey, "validate.mustRetainPermission");
            }
        }

        Set<Role> inherited = getAllInheritedRoles(holder);
        if (!inherited.contains(PermissionHolder.SUPERADMIN_ROLE)) {
            Set<Set<Role>> existingMinterms = existingPermission != null ? existingPermission.getRoles() : Collections.emptySet();
            // changed contains all minterms which have been added or removed
            Set<Set<Role>> changed = Sets.symmetricDifference(existingMinterms, newPermission.getRoles());

            for (Set<Role> minterm : changed) {
                if (minterm.contains(PermissionHolder.USER_ROLE)) {
                    result.addContextualMessage(contextKey, "validate.permission.cantAddRoleSuperadminOnly", PermissionHolder.USER_ROLE.getXid());
                }
                if (minterm.contains(PermissionHolder.ANONYMOUS_ROLE)) {
                    result.addContextualMessage(contextKey, "validate.permission.cantAddRoleSuperadminOnly", PermissionHolder.ANONYMOUS_ROLE.getXid());
                }
                Optional<Role> notHeldRole = minterm.stream().filter(r -> !inherited.contains(r)).findAny();
                //noinspection OptionalIsPresent
                if (notHeldRole.isPresent()) {
                    result.addContextualMessage(contextKey, "validate.permission.cantAddRole", notHeldRole.get().getXid());
                }
            }
        }
    }

    /**
     * Validate roles.  Used for things like a User or ScriptPermission. This will validate that:
     * <ol>
     *     <li>The user cannot remove a role they do not have</li>
     * </ol>
     *  @param result - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param currentUser - the current user
     * @param existingRoles - the previous roles
     * @param newRoles - the new roles to validate
     */
    public void validatePermissionHolderRoles(ProcessResult result, String contextKey, PermissionHolder currentUser,
            Set<Role> existingRoles, Set<Role> newRoles) {

        validatePermissionHolderRoles(result, contextKey, currentUser, newRoles);
        validateRemovedRoles(result, contextKey, currentUser, existingRoles, newRoles);
    }

    private void validateRemovedRoles(ProcessResult result, String contextKey, PermissionHolder currentUser,
            Set<Role> existingRoles, Set<Role> newRoles) {

        // if any roles were removed, validate if the removal is an authorized one
        Set<Role> removedRoles = existingRoles.stream().filter(e -> !newRoles.contains(e)).collect(Collectors.toSet());
        Set<Role> inherited = getAllInheritedRoles(currentUser);

        // the removed roles should be part of the holder's inherited set (or holder should be super admin)
        if (!inherited.contains(PermissionHolder.SUPERADMIN_ROLE) && !inherited.containsAll(removedRoles)) {
            result.addContextualMessage(contextKey, "validate.role.invalidRemoval", implodeRoles(inherited));
        }
    }

    /**
     * Validate roles.  Used for things like a User or ScriptPermission. This will validate that:
     * <ol>
     *     <li>The new roles are non null</li>
     *     <li>All new roles are not empty</li>
     *     <li>The new roles exist</li>
     *     <li>The user cannot assign a role they do not have</li>
     * </ol>
     *  @param result - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param currentUser - the current user
     * @param newRoles - the new roles to validate
     */
    public void validatePermissionHolderRoles(ProcessResult result, String contextKey,
                                              PermissionHolder currentUser, Set<Role> newRoles) {

        Assert.notNull(result, "result must not be null");
        Assert.notNull(contextKey, "contextKey must not be null");
        Assert.notNull(currentUser, "holder must not be null");

        if (newRoles == null) {
            result.addContextualMessage(contextKey, "validate.permission.null");
            return;
        }

        validateRoles(result, contextKey, newRoles);
        if (!result.isValid()) {
            return;
        }

        Set<Role> inherited = getAllInheritedRoles(currentUser);
        if (!inherited.contains(PermissionHolder.SUPERADMIN_ROLE) && !inherited.containsAll(newRoles)) {
            result.addContextualMessage(contextKey, "validate.role.invalidModification", implodeRoles(inherited));
        }
    }

    private boolean validateRoles(ProcessResult result, String contextKey, Set<Role> roles) {
        boolean valid = true;
        for (Role role : roles) {
            if (role.getXid() == null) {
                result.addContextualMessage(contextKey, "validate.role.empty");
                valid = false;
                continue;
            }
            Integer id = roleDao.getIdByXid(role.getXid());
            if (id == null) {
                result.addContextualMessage(contextKey, "validate.role.notFound", role.getXid());
                valid = false;
            } else if (id != role.getId()) {
                result.addContextualMessage(contextKey, "validate.role.invalidReference", role.getXid(), role.getId());
                valid = false;
            }
        }
        return valid;
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
     * TODO Mango 4.2 remove
     * Turn a set of RoleVOs into a comma separated list for display in a message
     * @param roles
     * @return
     */
    public static String implodeRoleVOs(Set<RoleVO> roles) {
        return String.join(",", roles.stream().map(AbstractVO::getXid).collect(Collectors.toSet()));
    }

    /**
     * TODO Mango 4.2 remove
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
    @SuppressWarnings("SpringEventListenerInspection")
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
    public void clearCaches(boolean force) {
        ensureAdminRole(Common.getUser());
        this.roleHierarchyCache.invalidateAll();
        this.permissionCache.clear();
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

    /**
     * TODO Mango 4.2 temporary fix to requiring this in all EventType.hasPermission() methods
     * @return
     */
    public boolean hasEventsSuperadminViewPermission(PermissionHolder holder) {
        return hasPermission(holder, this.eventsSuperadminViewPermissionDefinition.getPermission());
    }

    private static final class RoleInheritance {
        Role role;
        Set<Role> inherited;
        Set<Role> inheritedBy;
    }
}
