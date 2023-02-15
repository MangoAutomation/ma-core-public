/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.permission;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Container for a set of roles that apply to a permission such as 'permissionDatasource' or  'permissions.user.editSelf'
 * or a specific VO permission such as data point set permission.
 *
 * <p>Note: The id field is not considered in the {@link #hashCode()} and {@link #equals(java.lang.Object)} methods.</p>
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public final class MangoPermission {

    private final Integer id;

    @JsonProperty
    private final Set<Set<Role>> roles;

    /**
     * Creates a permission that only superadmins have access to
     */
    public MangoPermission() {
        this(null, Collections.emptySet());
    }

    /**
     * Creates a placeholder permission that just contains an id, used while loading VO objects from DB.
     * The permission contains an empty set of permissions so if any access control checks are performed against this
     * permission (this should not occur) only the superadmin will be granted access.
     */
    public MangoPermission(int id) {
        this(id, Collections.emptySet());
    }

    /**
     * Return a new permission with the same roles but with a new ID.
     */
    public MangoPermission(int id, MangoPermission permission) {
        this.id = id;
        // roles are already validated
        this.roles = permission.getRoles();
    }

    /**
     * Creates a permission for the given set of minterms.
     * @param minterms set of minterms
     */
    public MangoPermission(Set<Set<Role>> minterms) {
        this(null, minterms);
    }

    /**
     * Used when serializing a permission to store in a data column in the database.
     * @return array of minterms (array of role xids)
     */
    public String[][] toArray() {
        return roles.stream()
                .map(mt -> mt.stream().map(Role::getXid).toArray(String[]::new))
                .toArray(String[][]::new);
    }

    /**
     * Construct a permission that must already exist in the database
     */
    private MangoPermission(Integer id, Set<Set<Role>> minterms) {
        this.id = id;

        if (minterms == null) {
            throw new IllegalArgumentException("Roles cannot be null");
        }

        this.roles = Collections.unmodifiableSet(minterms.stream()
                .map(minterm -> {
                    if (minterm == null) {
                        throw new IllegalArgumentException("Minterm cannot be null");
                    }
                    if (minterm.isEmpty()) {
                        throw new IllegalArgumentException("Minterm cannot be empty");
                    }
                    if (minterm.contains(null)) {
                        throw new IllegalArgumentException("Minterm cannot contain null role");
                    }
                    return Collections.unmodifiableSet(new HashSet<>(minterm));
                })
                .collect(Collectors.toSet()));
    }

    /**
     * Return a new permission with the same roles but with a new ID.
     */
    public MangoPermission withId(Integer id) {
        return new MangoPermission(id, this);
    }

    public Set<Set<Role>> getRoles() {
        return roles;
    }

    public static MangoPermission superadminOnly() {
        return new MangoPermission();
    }

    public static MangoPermission requireAnyRole(Role ...roles) {
        return requireAnyRole(Arrays.stream(roles));
    }

    public static MangoPermission requireAnyRole(Set<Role> roles) {
        return requireAnyRole(roles.stream());
    }

    public static MangoPermission requireAnyRole(Stream<Role> roles) {
        return new MangoPermission(roles
                .map(Collections::singleton)
                .collect(Collectors.toSet()));
    }

    public static MangoPermission requireAllRoles(Role ...roles) {
        return requireAllRoles(Arrays.stream(roles));
    }

    public static MangoPermission requireAllRoles(Set<Role> roles) {
        return new MangoPermission(Collections.singleton(roles));
    }

    public static MangoPermission requireAllRoles(Stream<Role> roles) {
        return requireAllRoles(roles.collect(Collectors.toSet()));
    }

    public static MangoPermission merge(MangoPermission a, MangoPermission b) {
        var minterms = Stream.concat(a.getRoles().stream(), b.getRoles().stream()).collect(Collectors.toSet());
        return new MangoPermission(minterms);
    }

    /**
     * Does not depend on id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MangoPermission that = (MangoPermission) o;
        return roles.equals(that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roles);
    }

    @Override
    public String toString() {
        return "MangoPermission{" +
                roles.stream().map(minterm -> minterm.stream()
                        .map(Role::getXid)
                        .collect(Collectors.joining(" AND ", "(", ")")))
                .collect(Collectors.joining(" OR ")) +
                '}';
    }

    /**
     * Checks if this permission contains the role, if it does then it will return a new permission without that role.
     * The permission is never modified.
     *
     * @param role the role to remove
     * @return new permission or the existing one if it doesn't contain the role
     */
    public MangoPermission withoutRole(Role role) {
        boolean containsRole = roles.stream()
                .flatMap(Collection::stream)
                .anyMatch(role::equals);

        if (containsRole) {
            return new MangoPermission(roles.stream().map(minterm -> minterm.stream()
                    .filter(r -> !r.equals(role))
                    .collect(Collectors.toSet()))
                    .filter(minterm -> !minterm.isEmpty()).collect(Collectors.toSet()));
        }

        return this;
    }

    public MangoPermission merge(MangoPermission other) {
        return MangoPermission.merge(this, other);
    }

    public Integer getId() {
        return id;
    }

    public static MangoPermissionBuilder builder() {
        return new MangoPermissionBuilder();
    }

    public static final class MangoPermissionBuilder {
        Set<Set<Role>> minterms = new HashSet<>();

        private MangoPermissionBuilder() {}

        public MangoPermissionBuilder minterm(Role... roles) {
            minterms.add(Arrays.stream(roles).collect(Collectors.toSet()));
            return this;
        }

        public MangoPermissionBuilder minterm(Stream<Role> roles) {
            minterms.add(roles.collect(Collectors.toSet()));
            return this;
        }

        public MangoPermission build() {
            return new MangoPermission(minterms);
        }
    }
}
