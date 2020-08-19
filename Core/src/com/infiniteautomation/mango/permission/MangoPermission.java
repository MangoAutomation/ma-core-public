/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.permission;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.vo.role.Role;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Container for a set of roles that apply to a permission such as 'permissionDatasource' or  'permissions.user.editSelf'
 *   or a specific VO permission such as data point set permission.
 *
 *  @author Terry Packer
 *  @author Jared Wiltshire
 *
 */
public class MangoPermission {

    // TODO Mango 4.0 remove
    Integer id;

    @JsonProperty
    protected final Set<Set<Role>> roles;

    /**
     * Creates a permission that only superadmins have access to
     */
    public MangoPermission() {
        this(Collections.emptySet());
    }

    /**
     * Creates a permission that only superadmins have access to
     */
    public MangoPermission(int id) {
        this(Collections.emptySet());
        this.id = id;
    }

    /**
     * Creates a permission for the given set of minterms.
     * @param roles
     */
    public MangoPermission(Set<Set<Role>> roles) {
        if (roles == null) {
            throw new IllegalArgumentException("Roles cannot be null");
        }

        this.roles = Collections.unmodifiableSet(roles.stream()
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
                    return Collections.unmodifiableSet(minterm);
                })
                .collect(Collectors.toSet()));
    }

    public Set<Set<Role>> getRoles() {
        return roles;
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
                "roles=" + roles.stream().map(minterm -> {
            return minterm.stream()
                    .map(Role::getXid)
                    .collect(Collectors.joining(" AND ", "(", ")"));
        }).collect(Collectors.joining(" OR ")) +
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
            return new MangoPermission(roles.stream().map(minterm -> {
                return minterm.stream()
                        .filter(r -> !r.equals(role))
                        .collect(Collectors.toSet());
            }).filter(minterm -> !minterm.isEmpty()).collect(Collectors.toSet()));
        }

        return this;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
